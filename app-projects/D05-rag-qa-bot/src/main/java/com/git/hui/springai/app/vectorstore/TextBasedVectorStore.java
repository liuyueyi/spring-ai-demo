package com.git.hui.springai.app.vectorstore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.observation.conventions.VectorStoreProvider;
import org.springframework.ai.observation.conventions.VectorStoreSimilarityMetric;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.ai.vectorstore.AbstractVectorStoreBuilder;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStoreContent;
import org.springframework.ai.vectorstore.filter.FilterExpressionConverter;
import org.springframework.ai.vectorstore.filter.converter.SimpleVectorStoreFilterExpressionConverter;
import org.springframework.ai.vectorstore.observation.AbstractObservationVectorStore;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationContext;
import org.springframework.core.io.Resource;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;

/**
 * 基于文本匹配的VectorStore实现，不需要向量模型
 * 使用关键词匹配和文本相似度算法替代向量嵌入，用于简易的RAG验证场景
 */
public class TextBasedVectorStore extends AbstractObservationVectorStore {

    private static final Logger logger = LoggerFactory.getLogger(TextBasedVectorStore.class);
    private final ObjectMapper objectMapper = ((JsonMapper.Builder) JsonMapper.builder().addModules(JacksonUtils.instantiateAvailableModules())).build();
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final FilterExpressionConverter filterExpressionConverter = new SimpleVectorStoreFilterExpressionConverter();
    @Getter
    protected Map<String, SimpleVectorStoreContent> store = new ConcurrentHashMap();

    /**
     * 已经存储到向量库的document
     */
    private Set<String> persistMd5 = new CopyOnWriteArraySet<>();

    protected TextBasedVectorStore(TextBasedVectorStoreBuilder builder) {
        super(builder);
    }

    public static TextBasedVectorStoreBuilder builder() {
        return new TextBasedVectorStoreBuilder(new EmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                return null;
            }

            @Override
            public float[] embed(Document document) {
                return new float[0];
            }
        });
    }

    /**
     * 添加文档到向量数据库
     *
     * @param documents
     */
    @Override
    public void doAdd(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }

        // 创建一个新的可变列表副本，避免对不可变列表进行操作
        List<Document> mutableDocuments = new ArrayList<>();
        for (Document document : documents) {
            // 过滤掉重复的文档，避免二次写入，浪费空间
            if (!persistMd5.contains((String) document.getMetadata().get("md5"))) {
                mutableDocuments.add(document);
            }
        }

        if (CollectionUtils.isEmpty(mutableDocuments)) {
            return;
        }

        // 文档分片
        List<Document> chunkers = DocumentChunker.DEFAULT_CHUNKER.chunkDocuments(mutableDocuments);
        // 存储本地向量库
        chunkers.forEach(document -> {
            logger.info("quantizeDocument for document id = {}", document.getId());
            float[] embedding = DocumentQuantizer.quantizeDocument(document);
            if (embedding.length == 0) {
                return;
            }
            SimpleVectorStoreContent storeContent = new SimpleVectorStoreContent(document.getId(), document.getText(), document.getMetadata(), embedding);
            this.store.put(document.getId(), storeContent);
        });
        mutableDocuments.forEach(document -> persistMd5.add((String) document.getMetadata().get("md5")));
    }

    public void doDelete(List<String> idList) {
        Iterator var2 = idList.iterator();

        while (var2.hasNext()) {
            String id = (String) var2.next();
            this.store.remove(id);
        }
    }

    /**
     * 搜索向量数据库，根据相似度返回相关文档
     *
     * @param request
     * @return
     */
    @Override
    public List<Document> doSimilaritySearch(SearchRequest request) {
        Predicate<SimpleVectorStoreContent> documentFilterPredicate = this.doFilterPredicate(request);
        final float[] userQueryEmbedding = this.getUserQueryEmbedding(request.getQuery());
        return this.store.values().stream().filter(documentFilterPredicate).map((content) -> {
            return content.toDocument(DocumentQuantizer.calculateCosineSimilarity(userQueryEmbedding, content.getEmbedding()));
        }).filter((document) -> {
            logger.info("Document score: {} - {}", document.getId(), document.getScore());
            return document.getScore() >= request.getSimilarityThreshold();
        }).sorted(Comparator.comparing(Document::getScore).reversed()).limit((long) request.getTopK()).toList();
    }

    private Predicate<SimpleVectorStoreContent> doFilterPredicate(SearchRequest request) {
        return request.hasFilterExpression() ? (document) -> {
            StandardEvaluationContext context = new StandardEvaluationContext();
            context.setVariable("metadata", document.getMetadata());
            return (Boolean) this.expressionParser.parseExpression(this.filterExpressionConverter.convertExpression(request.getFilterExpression())).getValue(context, Boolean.class);
        } : (document) -> {
            return true;
        };
    }

    public void save(File file) {
        String json = this.getVectorDbAsJson();

        try {
            if (!file.exists()) {
                logger.info("Creating new vector store file: {}", file);

                try {
                    Files.createFile(file.toPath());
                } catch (FileAlreadyExistsException var11) {
                    throw new RuntimeException("File already exists: " + String.valueOf(file), var11);
                } catch (IOException var12) {
                    throw new RuntimeException("Failed to create new file: " + String.valueOf(file) + ". Reason: " + var12.getMessage(), var12);
                }
            } else {
                logger.info("Overwriting existing vector store file: {}", file);
            }

            OutputStream stream = new FileOutputStream(file);

            try {
                Writer writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);

                try {
                    writer.write(json);
                    writer.flush();
                } catch (Throwable var9) {
                    try {
                        writer.close();
                    } catch (Throwable var8) {
                        var9.addSuppressed(var8);
                    }

                    throw var9;
                }

                writer.close();
            } catch (Throwable var10) {
                try {
                    stream.close();
                } catch (Throwable var7) {
                    var10.addSuppressed(var7);
                }

                throw var10;
            }

            stream.close();
        } catch (IOException var13) {
            logger.error("IOException occurred while saving vector store file.", var13);
            throw new RuntimeException(var13);
        } catch (SecurityException var14) {
            logger.error("SecurityException occurred while saving vector store file.", var14);
            throw new RuntimeException(var14);
        } catch (NullPointerException var15) {
            logger.error("NullPointerException occurred while saving vector store file.", var15);
            throw new RuntimeException(var15);
        }
    }

    public void load(File file) {
        TypeReference<HashMap<String, SimpleVectorStoreContent>> typeRef = new TypeReference<HashMap<String, SimpleVectorStoreContent>>() {
        };

        try {
            this.store = (Map) this.objectMapper.readValue(file, typeRef);
        } catch (IOException var4) {
            throw new RuntimeException(var4);
        }
    }

    public void load(Resource resource) {
        TypeReference<HashMap<String, SimpleVectorStoreContent>> typeRef = new TypeReference<HashMap<String, SimpleVectorStoreContent>>() {
        };

        try {
            this.store = (Map) this.objectMapper.readValue(resource.getInputStream(), typeRef);
        } catch (IOException var4) {
            throw new RuntimeException(var4);
        }
    }

    private String getVectorDbAsJson() {
        ObjectWriter objectWriter = this.objectMapper.writerWithDefaultPrettyPrinter();

        try {
            return objectWriter.writeValueAsString(this.store);
        } catch (JsonProcessingException var3) {
            throw new RuntimeException("Error serializing documentMap to JSON.", var3);
        }
    }

    private float[] getUserQueryEmbedding(String query) {
        return DocumentQuantizer.quantizeQuery(query);
    }

    public VectorStoreObservationContext.Builder createObservationContextBuilder(String operationName) {
        return VectorStoreObservationContext.builder(VectorStoreProvider.SIMPLE.value(), operationName)
                .dimensions(getUserQueryEmbedding("Test String").length)
                .collectionName("in-memory-map")
                .similarityMetric(VectorStoreSimilarityMetric.COSINE.value());
    }

    public static final class TextBasedVectorStoreBuilder extends AbstractVectorStoreBuilder<TextBasedVectorStoreBuilder> {
        private TextBasedVectorStoreBuilder(EmbeddingModel embeddingModel) {
            super(embeddingModel);
        }

        public TextBasedVectorStore build() {
            return new TextBasedVectorStore(this);
        }
    }
}