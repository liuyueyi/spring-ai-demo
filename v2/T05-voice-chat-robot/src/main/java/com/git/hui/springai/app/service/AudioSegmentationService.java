package com.git.hui.springai.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 音频分割服务
 * 支持将大型音频文件分割成多个小片段以便并行处理
 *
 * @author YiHui
 * @date 2026/3/2
 */
@Service
public class AudioSegmentationService {
    private static final Logger log = LoggerFactory.getLogger(AudioSegmentationService.class);
    
    // 线程池用于并行处理
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    
    // 默认分割参数
    private static final int DEFAULT_SEGMENT_DURATION_MS = 30000; // 30秒
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    
    /**
     * 音频片段信息
     */
    public static class AudioSegment {
        private final byte[] data;
        private final int index;
        private final long startTimeMs;
        private final long endTimeMs;
        
        public AudioSegment(byte[] data, int index, long startTimeMs, long endTimeMs) {
            this.data = data;
            this.index = index;
            this.startTimeMs = startTimeMs;
            this.endTimeMs = endTimeMs;
        }
        
        // Getters
        public byte[] getData() { return data; }
        public int getIndex() { return index; }
        public long getStartTimeMs() { return startTimeMs; }
        public long getEndTimeMs() { return endTimeMs; }
    }
    
    /**
     * 分割策略枚举
     */
    public enum SegmentationStrategy {
        TIME_BASED,    // 基于时间分割
        SIZE_BASED,    // 基于文件大小分割
        SILENCE_BASED  // 基于静音检测分割（高级功能）
    }
    
    /**
     * 按时间分割音频文件
     * 
     * @param file 原始音频文件
     * @param segmentDurationMs 每个片段的时长（毫秒）
     * @return 音频片段列表
     */
    public List<AudioSegment> segmentByTime(MultipartFile file, int segmentDurationMs) throws IOException {
        log.info("开始按时间分割音频文件: {}, 片段时长: {}ms", file.getOriginalFilename(), segmentDurationMs);
        
        byte[] fileBytes = file.getBytes();
        List<AudioSegment> segments = new ArrayList<>();
        
        try {
            // 使用AudioSystem读取音频格式信息
            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(inputStream);
            
            AudioFormat format = audioStream.getFormat();
            long frameLength = audioStream.getFrameLength();
            float frameRate = format.getFrameRate();
            
            log.info("音频格式信息 - 采样率: {}Hz, 帧数: {}, 格式: {}", 
                    frameRate, frameLength, format.toString());
            
            // 计算每个片段的帧数
            long framesPerSegment = (long) (segmentDurationMs / 1000.0f * frameRate);
            int frameSize = format.getFrameSize();
            
            long totalFrames = frameLength > 0 ? frameLength : (long) (fileBytes.length / (double) frameSize);
            int totalSegments = (int) Math.ceil(totalFrames / (double) framesPerSegment);
            
            log.info("总帧数: {}, 每片段帧数: {}, 总片段数: {}", 
                    totalFrames, framesPerSegment, totalSegments);
            
            // 重置输入流
            inputStream.close();
            inputStream = new ByteArrayInputStream(fileBytes);
            audioStream = AudioSystem.getAudioInputStream(inputStream);
            
            // 分割音频
            byte[] buffer = new byte[(int) (framesPerSegment * frameSize)];
            long currentFrame = 0;
            
            for (int i = 0; i < totalSegments; i++) {
                long framesToRead = Math.min(framesPerSegment, totalFrames - currentFrame);
                int bytesRead = audioStream.read(buffer, 0, (int) (framesToRead * frameSize));
                
                if (bytesRead > 0) {
                    byte[] segmentData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, segmentData, 0, bytesRead);
                    
                    long startTimeMs = (long) (currentFrame / frameRate * 1000);
                    long endTimeMs = (long) ((currentFrame + framesToRead) / frameRate * 1000);
                    
                    segments.add(new AudioSegment(segmentData, i, startTimeMs, endTimeMs));
                    log.debug("创建音频片段 {}: 时间 {}ms - {}ms, 大小: {} bytes", 
                            i, startTimeMs, endTimeMs, bytesRead);
                    
                    currentFrame += framesToRead;
                }
            }
            
            audioStream.close();
            inputStream.close();
            
        } catch (UnsupportedAudioFileException e) {
            log.error("不支持的音频文件格式: {}", e.getMessage());
            throw new IOException("不支持的音频文件格式", e);
        }
        
        log.info("音频分割完成，共生成 {} 个片段", segments.size());
        return segments;
    }
    
    /**
     * 按文件大小分割音频
     * 
     * @param file 原始音频文件
     * @param maxSegmentSizeBytes 每个片段的最大字节数
     * @return 音频片段列表
     */
    public List<AudioSegment> segmentBySize(MultipartFile file, long maxSegmentSizeBytes) throws IOException {
        log.info("开始按大小分割音频文件: {}, 最大片段大小: {} bytes", 
                file.getOriginalFilename(), maxSegmentSizeBytes);
        
        byte[] fileBytes = file.getBytes();
        List<AudioSegment> segments = new ArrayList<>();
        
        int totalSegments = (int) Math.ceil(fileBytes.length / (double) maxSegmentSizeBytes);
        
        for (int i = 0; i < totalSegments; i++) {
            int start = (int) (i * maxSegmentSizeBytes);
            int end = (int) Math.min((i + 1) * maxSegmentSizeBytes, fileBytes.length);
            int length = end - start;
            
            byte[] segmentData = new byte[length];
            System.arraycopy(fileBytes, start, segmentData, 0, length);
            
            // 简化的时间计算（假设均匀分布）
            long startTimeMs = (long) (i * (fileBytes.length / (double) maxSegmentSizeBytes) * 1000);
            long endTimeMs = (long) ((i + 1) * (fileBytes.length / (double) maxSegmentSizeBytes) * 1000);
            
            segments.add(new AudioSegment(segmentData, i, startTimeMs, endTimeMs));
            log.debug("创建音频片段 {}: 字节 {}-{}, 大小: {} bytes", i, start, end, length);
        }
        
        log.info("音频按大小分割完成，共生成 {} 个片段", segments.size());
        return segments;
    }
    
    /**
     * 智能分割 - 根据文件大小和时长自动选择最优策略
     * 
     * @param file 原始音频文件
     * @return 音频片段列表
     */
    public List<AudioSegment> smartSegment(MultipartFile file) throws IOException {
        long fileSize = file.getSize();
        
        // 对于小文件直接处理，不分割
        if (fileSize <= MAX_FILE_SIZE_BYTES) {
            log.info("文件较小 ({} bytes)，直接处理无需分割", fileSize);
            return List.of(new AudioSegment(file.getBytes(), 0, 0, getFileDurationMs(file)));
        }
        
        // 对于大文件，按时间分割
        return segmentByTime(file, DEFAULT_SEGMENT_DURATION_MS);
    }
    
    /**
     * 并行处理音频片段
     * 
     * @param segments 音频片段列表
     * @param processor 片段处理器函数
     * @return 处理结果列表
     */
    public <T> List<CompletableFuture<T>> processSegmentsParallel(
            List<AudioSegment> segments, 
            java.util.function.Function<AudioSegment, T> processor) {
        
        log.info("开始并行处理 {} 个音频片段", segments.size());
        
        List<CompletableFuture<T>> futures = new ArrayList<>();
        
        for (AudioSegment segment : segments) {
            CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
                try {
                    log.debug("开始处理片段 {}", segment.getIndex());
                    T result = processor.apply(segment);
                    log.debug("片段 {} 处理完成", segment.getIndex());
                    return result;
                } catch (Exception e) {
                    log.error("处理片段 {} 时发生错误", segment.getIndex(), e);
                    throw new RuntimeException(e);
                }
            }, executorService);
            
            futures.add(future);
        }
        
        return futures;
    }
    
    /**
     * 等待所有并行任务完成并收集结果
     * 
     * @param futures CompletableFuture列表
     * @param <T> 结果类型
     * @return 按顺序排列的结果列表
     */
    public <T> List<T> collectResults(List<CompletableFuture<T>> futures) {
        List<T> results = new ArrayList<>();
        
        for (int i = 0; i < futures.size(); i++) {
            try {
                T result = futures.get(i).join(); // 按顺序等待结果
                results.add(result);
                log.debug("收集到片段 {} 的结果", i);
            } catch (Exception e) {
                log.error("收集片段 {} 结果时发生错误", i, e);
                results.add(null); // 保持索引对应关系
            }
        }
        
        return results;
    }
    
    /**
     * 获取音频文件时长（毫秒）
     */
    private long getFileDurationMs(MultipartFile file) throws IOException {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(file.getBytes());
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(inputStream);
            
            AudioFormat format = audioStream.getFormat();
            long frameLength = audioStream.getFrameLength();
            float frameRate = format.getFrameRate();
            
            long durationMs = (long) (frameLength / frameRate * 1000);
            audioStream.close();
            inputStream.close();
            
            return durationMs;
        } catch (UnsupportedAudioFileException e) {
            log.warn("无法获取音频时长: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * 关闭线程池
     */
    public void shutdown() {
        executorService.shutdown();
    }
}