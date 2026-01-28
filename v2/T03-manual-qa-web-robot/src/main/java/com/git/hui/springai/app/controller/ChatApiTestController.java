package com.git.hui.springai.app.controller;

import com.git.hui.springai.app.context.ReqContextHolder;
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author YiHui
 * @date 2026/1/28
 */
@RequestMapping("/api/test")
@RestController
public class ChatApiTestController {

    List<AskUserQuestionTool.Question> questions = List.of(
            new AskUserQuestionTool.Question("您希望红包封面是什么风格？", "风格偏好",
                    List.of(
                            new AskUserQuestionTool.Question.Option("传统国风 ", "使用中国传统文化元素，如祥云、福字、古典纹样"),
                            new AskUserQuestionTool.Question.Option("现代简约", "色彩鲜艳，线条简洁，有现代感和设计感"),
                            new AskUserQuestionTool.Question.Option("可爱卡通", "Q版卡通风格，可爱有趣，适合年轻人"),
                            new AskUserQuestionTool.Question.Option("新中式", "结合传统元素和现代设计，平衡美感")
                    ), false),
            new AskUserQuestionTool.Question("您希望使用什么颜色为主色调", "主色调",
                    List.of(
                            new AskUserQuestionTool.Question.Option("喜庆红", "红色为主，象征喜庆吉祥"),
                            new AskUserQuestionTool.Question.Option("富贵金", "金色为主，寓意富贵吉祥"),
                            new AskUserQuestionTool.Question.Option("典雅蓝", "蓝色为主，象征高贵典雅"),
                            new AskUserQuestionTool.Question.Option("多彩", "多种颜色搭配，活泼有趣")
                    ), false),
            new AskUserQuestionTool.Question("您希望包含哪些设计元素?", "设计元素",
                    List.of(
                            new AskUserQuestionTool.Question.Option("骏马图案", "奔腾的马匹形象，寓意马到成功"),
                            new AskUserQuestionTool.Question.Option("祝福文字", "配上祝福语，如'马到成功'、'一马当先'等"),
                            new AskUserQuestionTool.Question.Option("传统纹样", "使用传统纹样，如祥云、波浪纹等"),
                            new AskUserQuestionTool.Question.Option("生肖元素", "加入生肖元素，如马年标识")
                    ), false)
    );

    private static final String RES = """
            太好了！根据您的选择，我为您设计了一个**新中式风格、喜庆红主色调、以生肖元素为主的马年红包封面**。
                        
            ## 🎨 设计方案
                        
            ### 🖼️ 整体构图
            - **背景**：渐变喜庆红（从深红到浅红），营造层次感
            - **中心**：一匹奔腾的骏马剪影，采用传统水墨画风格与现代简约线条结合
                        
            ### 🎯 设计元素
            1. **主图**：
            - 骏马呈向前奔跑姿态，线条流畅有力
            - 马鬃飘逸，展现动感
            - 马蹄腾空，寓意"马到成功"
                        
            2. **文字**：
            - 顶部：小号书法字体"马年大吉"
            - 中间：大号金色字体"发红包"
            - 底部：小号文字"2024"
                        
            3. **装饰**：
            - 四角：传统祥云纹样，增加文化韵味
            - 边框：简约金色线条，勾勒红包轮廓
                        
            ### 🎨 色彩搭配
            - **主色**：喜庆红（#D32F2F）- 营造节日氛围
            - **点缀色**：金色（#FFD700）- 象征富贵吉祥
            - **辅助色**：深棕色（#5D4037）- 增加传统质感
                        
            ### ✨ 设计亮点
            - 新中式风格既保留了传统文化底蕴，又符合现代审美
            - 奔腾的骏马寓意积极向上、事业顺利
            - 喜庆红与金色的搭配，喜庆又不失贵气
            - 简约的线条和纹样，适合微信红包的竖版尺寸
                        
            这个设计既适合个人使用，也适合商务场合，传递出吉祥如意、马到成功的美好寓意！
                        
            您觉得这个设计方案如何？如果需要调整任何细节，请告诉我！""";

    @GetMapping(path = "/chat/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startChat(@PathVariable("chatId") String chatId,
                                @RequestParam("question") String question) {
        SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);
        ReqContextHolder.setReqId(new ReqContextHolder.ReqInfo(chatId, sseEmitter));

        // 启动异步线程处理SSE响应
        Thread thread = new Thread(() -> {
            try {
                for (AskUserQuestionTool.Question q : questions) {
                    sendMsg(sseEmitter, "\n" + q.header() + ": " + q.question() + "\n");

                    List<AskUserQuestionTool.Question.Option> options = q.options();
                    for (int i = 0; i < options.size(); i++) {
                        AskUserQuestionTool.Question.Option opt = options.get(i);
                        sendMsg(sseEmitter, String.format("  %d. %s - %s%n", i + 1, opt.label(), opt.description()));
                    }

                    if (q.multiSelect()) {
                        sendMsg(sseEmitter, "  (Enter numbers separated by commas, or type custom text)");
                    } else {
                        sendMsg(sseEmitter, "  (Enter a number, or type custom text)");
                    }

                    // 阻塞等待用户输入
                    BlockingQueue<String> queue = chatHistory.get(chatId);
                    if (queue == null) {
                        queue = new LinkedBlockingQueue<>();
                        chatHistory.put(chatId, queue);
                    }
                    String response = null;
                    try {
                        // 等待最多5秒，如果超时则返回空字符串
                        response = queue.poll(5, TimeUnit.MINUTES);
                        if (response == null) {
                            response = ""; // 超时情况下的默认响应
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        response = "";
                    }
                }

                try {
                    String content = RES;
                    System.out.println("---> 接收大模型返回: " + content.replaceAll("\n", "\t"));
                    sseEmitter.send(content);

                    // 发送结束信号
                    sseEmitter.send(SseEmitter.event().name("done").data(""));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // 等待一段时间以确保前端收到done事件
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    sseEmitter.send(SseEmitter.event()
                            .name("error")
                            .data("Exception occurred: " + e.getMessage()));
                } catch (IOException ioException) {
                    throw new RuntimeException(ioException);
                }
            } finally {
                sseEmitter.complete();
                ReqContextHolder.clear();
            }
        });

        thread.start();

        return sseEmitter;
    }

    private Map<String, BlockingQueue<String>> chatHistory = new ConcurrentHashMap<>();

    /**
     * 用户给大模型发送的消息
     *
     * @param chatId
     * @param msg
     * @return
     */
    @GetMapping(path = "/send/{chatId}")
    public Boolean sendMsg(@PathVariable("chatId") String chatId, @RequestParam String msg) {
        BlockingQueue<String> history = chatHistory.get(chatId);
        if (history == null) {
            chatHistory.put(chatId, new LinkedBlockingQueue<>());
        }
        chatHistory.get(chatId).add(msg);
        return true;
    }

    @GetMapping(path = "/genImg")
    public String genImg(@RequestParam String msg) throws IOException {
        return "https://spring.hhui.top/spring-blog/imgs/info/wx.jpg";
    }

    private void sendMsg(SseEmitter sseEmitter, String msg) {
        try {
            sseEmitter.send(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}