package com.git.hui.springai.app.controller;

import com.git.hui.springai.app.advisor.MyLoggingAdvisor;
import com.git.hui.springai.app.context.ReqContextHolder;
import com.git.hui.springai.app.util.QwenImgGen;
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
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
@RequestMapping("/api")
@RestController
public class ChatApiController {

    private final ChatClient chatClient;

    private final ImageModel imgModel;

    @Autowired
    private Environment environment;

    public ChatApiController(ChatClient.Builder chatClientBuilder, ImageModel imgModel) {
        this.imgModel = imgModel;
        this.chatClient = chatClientBuilder
                .defaultTools(AskUserQuestionTool.builder()
                        .questionHandler(new WebQuestionHandler())
                        .build())

                .defaultAdvisors(
                        // Tool calling advisor
                        ToolCallAdvisor.builder().conversationHistoryEnabled(false).build(),
                        // Chat memory advisor - after the tool calling advisor to remember tool calls
                        MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().maxMessages(500).build()).build(),
                        // Custom logging advisor
                        MyLoggingAdvisor.builder()
                                .showAvailableTools(true)
                                .showSystemMessage(true)
                                .build())
                .build();
    }

    @GetMapping(path = "/chat/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startChat(@PathVariable("chatId") String chatId,
                                @RequestParam("question") String question) {
        SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);
        ReqContextHolder.setReqId(new ReqContextHolder.ReqInfo(chatId, sseEmitter));

        // 启动异步线程处理SSE响应
        Thread thread = new Thread(() -> {
            try {
                try {
                    String content = chatClient.prompt(question)
                            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                            .call()
                            .content();
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


    /**
     * 生成图片
     *
     * @param msg
     * @return
     * @throws IOException
     */
    @GetMapping(path = "/genImg")
    public String genImg(@RequestParam String msg) throws IOException {
        if ("true".equals(environment.getProperty("spring.ai.dashboard.enable"))) {
            return QwenImgGen.call(environment.getProperty("spring.ai.dashboard.api-key"), msg);
        } else {
            // 这里使用的是智谱的文生图模型，效果较差
            ImageResponse response = imgModel.call(new ImagePrompt(msg,
                    ImageOptionsBuilder.builder()
                            .height(1344)
                            .width(768)
                            .model("CogView-3-Flash")
                            // 返回图片类型
                            .responseFormat("png")
                            // 图像风格，如 vivid 生动风格， natural 自然风格
                            .style("natural")
                            .build())
            );
            Image img = response.getResult().getOutput();
            BufferedImage image = ImageIO.read(new URL(img.getUrl()));

            // 将图片转换为Base64编码
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            return java.util.Base64.getEncoder().encodeToString(imageBytes);
        }
    }

    public class WebQuestionHandler implements AskUserQuestionTool.QuestionHandler {
        @Override
        public Map<String, String> handle(List<AskUserQuestionTool.Question> questions) {
            Map<String, String> answers = new HashMap<>();
            ReqContextHolder.ReqInfo req = ReqContextHolder.getReqId();
            SseEmitter sseEmitter = req.sse();

            for (AskUserQuestionTool.Question q : questions) {
                sendMsg(sseEmitter, "\n" + q.header() + ": " + q.question());

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
                BlockingQueue<String> queue = chatHistory.get(req.chatId());
                if (queue == null) {
                    queue = new LinkedBlockingQueue<>();
                    chatHistory.put(req.chatId(), queue);
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

                answers.put(q.question(), parseResponse(response, options));
            }

            return answers;
        }

        private void sendMsg(SseEmitter sseEmitter, String msg) {
            try {
                sseEmitter.send(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private static String parseResponse(String response, List<AskUserQuestionTool.Question.Option> options) {
            try {
                // Try parsing as option number(s)
                String[] parts = response.split(",");
                List<String> labels = new ArrayList<>();
                for (String part : parts) {
                    int index = Integer.parseInt(part.trim()) - 1;
                    if (index >= 0 && index < options.size()) {
                        labels.add(options.get(index).label());
                    }
                }
                return labels.isEmpty() ? response : String.join(", ", labels);
            } catch (NumberFormatException e) {
                // Not a number, use as free text
                return response;
            }
        }

    }
}