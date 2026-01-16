package com.git.hui.springai.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.git.hui.springai.app.entity.invoice.BaseInvoiceInfo;
import com.git.hui.springai.app.entity.invoice.InvoiceInfo;
import com.git.hui.springai.app.entity.invoice.InvoiceItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.content.Media;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 发票提取服务
 *
 * @author YiHui
 * @date 2026/1/15
 */
@Slf4j
@Service
public class InvoiceExtractionService {

    @Value("classpath:/prompts/invoice-extract.st")
    private Resource invoiceSystemPrompt;

    @Value("classpath:/prompts/invoice-basic-extract.st")
    private Resource baseInvoiceSystemPrompt;

    @Value("classpath:/prompts/invoice-items-extract.st")
    private Resource invoiceItemPrompt;

    private final ChatModel chatModel;
    private final ChatClient chatClient;

    public InvoiceExtractionService(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }


    /**
     * 识别发票内容
     *
     * @param imageBytes 图片字节
     * @param mimeType   图片类型
     * @param msg        识别提示信息
     * @return
     */
    public InvoiceInfo extractInvoice(byte[] imageBytes, MimeType mimeType, String msg) {
        long start = System.currentTimeMillis();
        Message systemMsg = new SystemMessage(invoiceSystemPrompt);
        String message = (msg != null && !msg.isEmpty()) ? msg : "请将发票图片内容进行识别，并返回结构化的发票信息";

        Media media = Media.builder()
                .mimeType(mimeType)
                .data(imageBytes)
                .build();
        Message userMsg = UserMessage.builder().text(message).media(media).build();

        Prompt prompt = new Prompt(List.of(systemMsg, userMsg));
        InvoiceInfo invoiceInfo = chatClient.prompt(prompt).call().entity(InvoiceInfo.class);
        log.info("解析耗时：{} 返回: \n{}", System.currentTimeMillis() - start, toStr(invoiceInfo));
        return invoiceInfo;
    }


    /**
     * 使用上下文窗口更小的GLM-4V-Flash模型实现快速提取，适用于发票信息较小的情况
     *
     * @param imageBytes
     * @param mimeType
     * @param msg
     * @return
     */
    public InvoiceInfo fastExtractInvoice(byte[] imageBytes, MimeType mimeType, String msg) {
        BeanOutputConverter<InvoiceInfo> beanOutputConverter = new BeanOutputConverter<>(InvoiceInfo.class);
        String format = beanOutputConverter.getFormat();

        long start = System.currentTimeMillis();
        Message systemMsg = new SystemMessage(invoiceSystemPrompt);
        String message = (msg != null && !msg.isEmpty()) ? msg : "请将发票图片内容进行识别，并返回结构化的发票信息";

        Media media = Media.builder()
                .mimeType(mimeType)
                .data(imageBytes)
                .build();
        PromptTemplate template = new PromptTemplate("{message} \n返回格式:{format}");
        Message userMsg = UserMessage.builder()
                .text(template.render(Map.of("message", message, "format", format)))
                .media(media).build();

        Prompt prompt = new Prompt(List.of(systemMsg, userMsg), ChatOptions.builder()
                .model("GLM-4V-Flash")
                .temperature(0.1d)
                .maxTokens(1024)
                .build());

        Generation generation = chatModel.call(prompt).getResult();
        if (generation == null) {
            return null;
        }
        InvoiceInfo invoiceInfo = beanOutputConverter.convert(generation.getOutput().getText());
        log.info("解析耗时：{} 识别结果: \n{}", System.currentTimeMillis() - start, toStr(invoiceInfo));
        return invoiceInfo;
    }

    /**
     * 识别发票基础内容（不包含商品明细）
     *
     * @param imageBytes 图片字节
     * @param mimeType   图片类型
     * @param msg        识别提示信息
     * @return
     */
    public BaseInvoiceInfo extractBaseInvoice(byte[] imageBytes, MimeType mimeType, String msg) {
        Message systemMsg = new SystemMessage(baseInvoiceSystemPrompt);
        String message = (msg != null && !msg.isEmpty()) ? msg : "请将发票图片内容进行识别，并返回结构化的发票信息";

        Media media = Media.builder()
                .mimeType(mimeType)
                .data(imageBytes)
                .build();
        Message userMsg = UserMessage.builder().text(message).media(media).build();
        Prompt prompt = new Prompt(List.of(systemMsg, userMsg));
        BaseInvoiceInfo invoiceInfo = chatClient.prompt(prompt).call().entity(BaseInvoiceInfo.class);
        log.info("解析的结果: \n{}", toStr(invoiceInfo));
        return invoiceInfo;
    }

    /**
     * 识别发票中商品行较多的发票内容
     * <p>
     * - 图片中的信息，超过窗口上下文的场景，我们需要进行分批处理
     *
     * @param imageBytes 图片字节
     * @param mimeType   图片类型
     * @param msg        识别提示信息
     * @return
     */
    public InvoiceInfo extractInvoiceWitInhItems(byte[] imageBytes, MimeType mimeType, String msg) {
        Media media = Media.builder()
                .mimeType(mimeType)
                .data(imageBytes)
                .build();
        CompletableFuture<BaseInvoiceInfo> infoFuture = CompletableFuture.supplyAsync(() -> {
            BaseInvoiceInfo invoiceInfo = extractBaseInvoice(imageBytes, mimeType, msg);
            return invoiceInfo;
        });
        CompletableFuture<List<InvoiceItem>> itemFuture = CompletableFuture.supplyAsync(() -> {
            final int step = 5;
            int start = 0, end = step;
            List<InvoiceItem> totalItems = new ArrayList<>();
            while (true) {
                log.info("开始处理：{} - {}", start, end);
                List<InvoiceItem> items = extractInvoiceItems(media, start, end);
                if (CollectionUtils.isEmpty(items)) {
                    break;
                }
                totalItems.addAll(items);
                if (items.size() < end - start) {
                    break;
                } else {
                    start += step;
                    end += step;
                }
            }
            return totalItems;
        });

        // 等待两个任务完成
        CompletableFuture.allOf(infoFuture, itemFuture).join();

        InvoiceInfo invoiceInfo = new InvoiceInfo();
        try {
            BeanUtils.copyProperties(infoFuture.get(), invoiceInfo);
            invoiceInfo.setItems(itemFuture.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return invoiceInfo;
    }

    private List<InvoiceItem> extractInvoiceItems(Media media, Integer start, Integer end) {
        PromptTemplate promptTemplate = PromptTemplate.builder()
                // 因为提示词中返回的json对象中，有 {}，所以使用默认的 {} 来替换占位变量，会报错
                .renderer(StTemplateRenderer.builder().startDelimiterToken('$').endDelimiterToken('$').build())
                .resource(invoiceItemPrompt)
                .build();
        String sys = promptTemplate.render(Map.of("start", start, "end", end));
        SystemMessage systemMsg = new SystemMessage(sys);

        UserMessage userMsg = UserMessage.builder().media(media).text("提取" + start + "行到" + end + "行发票商品信息，注意不包含第" + end + "行").build();
        Prompt prompt = new Prompt(List.of(systemMsg, userMsg));
        List<InvoiceItem> items = chatClient.prompt(prompt).call().entity(new ParameterizedTypeReference<List<InvoiceItem>>() {
        });
        return items;
    }

    private String toStr(Object invoiceInfo) {
        ObjectMapper objectMapper = new ObjectMapper();
        // 注册Java 8日期时间模块以支持LocalDate等类型
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        try {
            return objectMapper.writeValueAsString(invoiceInfo);
        } catch (Exception e) {
            log.error("Error processing invoice info: " + e.getMessage(), e);
            return "Error processing invoice info: " + e.getMessage();
        }
    }
}
