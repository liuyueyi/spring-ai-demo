package com.git.hui.springai.app.service;

import org.springframework.ai.audio.transcription.AudioTranscriptionOptions;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 音频内容识别服务
 * 支持单个音频处理和并行分段处理
 *
 * @author YiHui
 * @date 2026/2/27
 */
@Service
public class AudioTransactionService {
    private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AudioTransactionService.class);
    @Autowired
    private TranscriptionModel transcriptionModel;
    
    @Autowired
    private AudioSegmentationService segmentationService;

    /**
     * 单个音频文件转录（原有方法保持兼容）
     */
    public String audioTransaction(MultipartFile file) throws IOException {
        return audioTransactionSingle(file);
    }
    
    /**
     * 并行处理音频文件（智能分割）
     * 
     * @param file 音频文件
     * @param useParallel 是否使用并行处理
     * @return 转录结果
     */
    public String audioTransactionParallel(MultipartFile file, boolean useParallel) throws IOException {
        if (!useParallel) {
            return audioTransactionSingle(file);
        }
        
        long startTime = System.currentTimeMillis();
        log.info("开始并行处理音频文件: {}", file.getOriginalFilename());
        
        try {
            // 智能分割音频
            List<AudioSegmentationService.AudioSegment> segments = segmentationService.smartSegment(file);
            
            if (segments.size() <= 1) {
                log.info("音频文件较小，直接处理");
                return audioTransactionSingle(file);
            }
            
            // 并行处理所有片段
            List<CompletableFuture<String>> futures = segmentationService.processSegmentsParallel(
                segments,
                this::transcribeSegment
            );
            
            // 收集结果
            List<String> results = segmentationService.collectResults(futures);
            
            // 合并结果
            String finalResult = mergeTranscriptionResults(results);
            
            long endTime = System.currentTimeMillis();
            log.info("并行处理完成，总耗时: {}ms，片段数: {}，结果长度: {}字符", 
                    endTime - startTime, segments.size(), finalResult.length());
            
            return finalResult;
            
        } catch (Exception e) {
            log.error("并行处理音频文件失败，回退到单线程处理", e);
            return audioTransactionSingle(file);
        }
    }
    
    /**
     * 单线程音频转录
     */
    private String audioTransactionSingle(MultipartFile file) throws IOException {
        long startTime = System.currentTimeMillis();
        log.info("开始单线程处理音频文件: {}", file.getOriginalFilename());
        
        AudioTranscriptionOptions options = OpenAiAudioTranscriptionOptions.builder()
                .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.JSON)
                .model("FunAudioLLM/SenseVoiceSmall").build();

        byte[] bytes = file.getBytes();
        Resource resource = createResourceFromFile(file, bytes);

        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(resource, options);
        AudioTranscriptionResponse response = transcriptionModel.call(prompt);
        
        long endTime = System.currentTimeMillis();
        log.info("单线程处理完成，耗时: {}ms", endTime - startTime);
        
        return response.getResult().getOutput();
    }
    
    /**
     * 转录单个音频片段
     */
    private String transcribeSegment(AudioSegmentationService.AudioSegment segment) {
        try {
            AudioTranscriptionOptions options = OpenAiAudioTranscriptionOptions.builder()
                    .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.JSON)
                    .model("FunAudioLLM/SenseVoiceSmall")
                    .build();
            
            Resource resource = new ByteArrayResource(segment.getData()) {
                @Override
                public String getFilename() {
                    return "segment_" + segment.getIndex() + ".tmp";
                }
                
                @Override
                public long contentLength() {
                    return segment.getData().length;
                }
            };
            
            AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(resource, options);
            AudioTranscriptionResponse response = transcriptionModel.call(prompt);
            
            String result = response.getResult().getOutput();
            log.debug("片段 {} 转录完成，结果长度: {} 字符", segment.getIndex(), result.length());
            
            return result;
        } catch (Exception e) {
            log.error("转录片段 {} 失败", segment.getIndex(), e);
            return ""; // 返回空字符串而不是抛出异常
        }
    }
    
    /**
     * 合并多个转录结果
     */
    private String mergeTranscriptionResults(List<String> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }
        
        // 过滤掉空结果
        List<String> validResults = results.stream()
                .filter(result -> result != null && !result.trim().isEmpty())
                .collect(Collectors.toList());
        
        if (validResults.isEmpty()) {
            return "";
        }
        
        // 简单的文本合并，可以用空格连接
        String mergedResult = String.join(" ", validResults);
        
        // 清理多余的空白字符
        mergedResult = mergedResult.replaceAll("\s+", " ").trim();
        
        log.info("合并转录结果: 共 {} 个有效片段，合并后长度: {} 字符", 
                validResults.size(), mergedResult.length());
        
        return mergedResult;
    }
    
    /**
     * 创建Resource对象
     */
    private Resource createResourceFromFile(MultipartFile file, byte[] bytes) {
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }

            @Override
            public long contentLength() {
                return file.getSize();
            }
        };
    }
}
