package com.git.hui.springai.app.qa;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.util.StringUtils;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author YiHui
 * @date 2026/1/21
 */
@Service
public class QaBoltService {
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    private final VectorStore vectorStore;
    /**
     * 提示语
     */
    @Value("classpath:/prompts/qa-prompts.pt")
    private Resource boltPrompts;

    public QaBoltService(ChatClient.Builder builder, VectorStore vectorStore, ChatMemory chatMemory) {
        this.vectorStore = vectorStore;
        this.chatMemory = chatMemory;
        this.chatClient = builder.defaultAdvisors(
                // 记录日志
                new SimpleLoggerAdvisor(ModelOptionsUtils::toJsonStringPrettyPrinter, ModelOptionsUtils::toJsonStringPrettyPrinter, 0),
                // 上下文窗口
                MessageChatMemoryAdvisor.builder(chatMemory).build(),
                // RAG https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html#_pre_retrieval
                RetrievalAugmentationAdvisor.builder()
                        .queryTransformers(
                                RewriteQueryTransformer.builder().chatClientBuilder(builder.build().mutate()).build()
                        )
                        .queryAugmenter(
                                ContextualQueryAugmenter.builder().allowEmptyContext(true).build()
                        )
                        .documentRetriever(
                                VectorStoreDocumentRetriever.builder()
                                        .similarityThreshold(0.50)
                                        .vectorStore(vectorStore)
                                        .build()
                        )
                        .build()
        ).build();
    }

    /**
     * 问答
     *
     * @param chatId   对话id，用于存储上下文信息
     * @param question 问题
     * @param files    用户上传的文件
     * @return
     */
    public Flux<String> ask(String chatId, String question, Collection<MultipartFile> files) {
        // 将附件保存到向量库
        processFiles(chatId, files);

        // 自定义的提示词模板，替换默认的检索参考资料的提示词模板
        // 其中 <query> 对应的是用户的提问 question
        // <question_answer_context> 对应的是增强检索的document，即检索到的参考资料
        PromptTemplate customPromptTemplate = PromptTemplate.builder()
                .renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
                .template("""
                        <query>

                        Context information is below.

                         ---------------------
                         <question_answer_context>
                         ---------------------

                         Given the context information and no prior knowledge, answer the query.

                         Follow these rules:

                         1. If the answer is not in the context, just say that you don't know.
                         2. Avoid statements like "Based on the context..." or "The provided information...".
                                  """).build();

        var qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder().similarityThreshold(0.5d).topK(3).build())
                .promptTemplate(customPromptTemplate)
                .build();
        var requestSpec = chatClient.prompt()
                .system(boltPrompts)
                .user(question)
                .advisors(qaAdvisor)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId));
        return requestSpec.stream().content().map(s -> s.replaceAll("\n", "<br/>"));
    }

    private static final String ATTACHMENT_TEMPLATE =
            """
                    <attachment filename="%s">
                            %s
                    </attachment>
                    """;

    private ProceedInfo processFiles(String chatId, Collection<MultipartFile> files) {
        StringBuilder context = new StringBuilder("\n\n");
        List<Media> mediaList = new ArrayList<>();
        files.forEach(file -> {
            try {
                var data = new ByteArrayResource(file.getBytes());
                // 计算hash值、避免重复保存到向量库中
                var md5 = calculateHash(chatId, file.getBytes());
                MimeType mime = MimeType.valueOf(file.getContentType());
                if (mime.equalsTypeAndSubtype(MediaType.APPLICATION_PDF)) {
                    // pdf
                    PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(data,
                            PdfDocumentReaderConfig.builder()
                                    .withPageTopMargin(0)
                                    .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                            .withNumberOfTopTextLinesToDelete(0)
                                            .build())
                                    .withPagesPerDocument(1)
                                    .build());
                    List<Document> documents = pdfReader.read();
                    documents.forEach(document -> {
                        document.getMetadata().put("md5", md5);
                        if (document.getMetadata().containsKey("file_name") && document.getMetadata().get("file_name") == null) {
                            document.getMetadata().put("file_name", file.getName());
                        }
                    });
                    vectorStore.add(documents);

                    var content = String.join("\n", documents.stream().map(Document::getText).toList());
                    context.append(String.format(ATTACHMENT_TEMPLATE, file.getName(), content));
                } else if ("text".equalsIgnoreCase(mime.getType())) {
                    // Apache Tika: PDF, DOC/DOCX, PPT/PPTX, and HTML （所以上面的pdf其实也可以用下面这个来实现）
                    List<Document> documents = new TikaDocumentReader(data).read();
                    documents.forEach(document -> document.getMetadata().put("md5", md5));
                    vectorStore.add(documents);

                    var content = String.join("\n", documents.stream().map(Document::getText).toList());
                    context.append(String.format(ATTACHMENT_TEMPLATE, file.getName(), content));
                } else if ("image".equalsIgnoreCase(mime.getType())) {
                    // image；现在的聊天机器人不支持图片，所以这里不做处理
                    mediaList.add(new Media(mime, data));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return new ProceedInfo(context.toString(), mediaList);
    }

    private String calculateHash(String chatId, byte[] bytes) {
        // 计算hash值、避免重复保存
        var md5 = DigestUtils.md5DigestAsHex(bytes);
        if (!StringUtils.isEmpty(chatId)) {
            md5 = chatId + "_" + md5;
        }
        return md5;
    }

    record ProceedInfo(String content, List<Media> mediaList) {
    }
}
