package com.git.hui.springai.app.demo;

import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档分块工具类
 * 将长文档分割成较小的块，以便更好地进行向量化和检索
 */
public class DocumentChunker {
    
    private final int maxChunkSize;
    private final int overlapSize;

    public static DocumentChunker DEFAULT_CHUNKER = new DocumentChunker();
    
    public DocumentChunker() {
        this(500, 50); // 默认值：最大块大小500个字符，重叠50个字符
    }
    
    public DocumentChunker(int maxChunkSize, int overlapSize) {
        this.maxChunkSize = maxChunkSize;
        this.overlapSize = overlapSize;
    }
    
    /**
     * 将文档分割成块
     * 
     * @param document 输入文档
     * @return 分割后的文档块列表
     */
    public List<Document> chunkDocument(Document document) {
        String content = document.getText();
        if (content == null || content.trim().isEmpty()) {
            return List.of(document);
        }
        
        List<String> chunks = splitText(content);
        List<Document> chunkedDocuments = new ArrayList<>();
        
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            String chunkId = document.getId() + "_chunk_" + i;
            
            // 创建新的文档块，保留原始文档的元数据
            Document chunkDoc = new Document(chunkId, chunk, new java.util.HashMap<>(document.getMetadata()));
            
            // 添加块相关的元数据
            chunkDoc.getMetadata().put("chunk_index", i);
            chunkDoc.getMetadata().put("total_chunks", chunks.size());
            chunkDoc.getMetadata().put("original_document_id", document.getId());
            
            chunkedDocuments.add(chunkDoc);
        }
        
        return chunkedDocuments;
    }
    
    /**
     * 将文本分割成块
     * 
     * @param text 输入文本
     * @return 分割后的文本块列表
     */
    private List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        
        // 按多种分隔符分割，优先在语义边界处分割（包括中文句号、问号、感叹号等）
        String[] sentences = text.split("(?<=。)|(?<=！)|(?<=!)|(?<=？)|(?<=\\?)|(?<=\\n\\n)");
        
        StringBuilder currentChunk = new StringBuilder();
        
        for (String sentence : sentences) {
            // 跳过空句子
            if (sentence.trim().isEmpty()) {
                continue;
            }
            
            // 如果当前块加上新句子不超过最大大小，就添加到当前块
            if (currentChunk.length() + sentence.length() <= maxChunkSize) {
                if (currentChunk.length() > 0) {
                    currentChunk.append(sentence);
                } else {
                    currentChunk.append(sentence);
                }
            } else {
                // 如果当前块为空，但是单个句子太长，需要强制分割
                if (currentChunk.length() == 0) {
                    List<String> subChunks = forceSplit(sentence, maxChunkSize);
                    for (int i = 0; i < subChunks.size(); i++) {
                        String subChunk = subChunks.get(i);
                        // 如果不是最后一个子块，添加到当前块并保存
                        if (i < subChunks.size() - 1) {
                            chunks.add(subChunk);
                        } else {
                            currentChunk.append(subChunk);
                        }
                    }
                } else {
                    // 保存当前块
                    chunks.add(currentChunk.toString());
                    // 开始新块，包含重叠部分
                    currentChunk = new StringBuilder();
                    
                    // 添加重叠部分，如果句子长度大于重叠大小，则只取末尾部分
                    if (sentence.length() > overlapSize) {
                        String overlap = sentence.substring(Math.max(0, sentence.length() - overlapSize));
                        currentChunk.append(overlap);
                        currentChunk.append(sentence);
                    } else {
                        currentChunk.append(sentence);
                    }
                }
            }
        }
        
        // 添加最后一个块
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        
        return chunks;
    }
    
    /**
     * 强制将长文本分割成指定大小的块
     * 
     * @param text 输入文本
     * @param maxSize 最大块大小
     * @return 分割后的文本块列表
     */
    private List<String> forceSplit(String text, int maxSize) {
        List<String> chunks = new ArrayList<>();
        
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + maxSize, text.length());
            String chunk = text.substring(start, end);
            chunks.add(chunk);
            start = end;
        }
        
        return chunks;
    }
    
    /**
     * 将多个文档分别分割成块
     * 
     * @param documents 输入文档列表
     * @return 分割后的文档块列表
     */
    public List<Document> chunkDocuments(List<Document> documents) {
        List<Document> allChunks = new ArrayList<>();
        
        for (Document document : documents) {
            allChunks.addAll(chunkDocument(document));
        }
        
        return allChunks;
    }
}