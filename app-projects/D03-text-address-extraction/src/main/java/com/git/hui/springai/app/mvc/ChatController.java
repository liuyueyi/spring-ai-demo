package com.git.hui.springai.app.mvc;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.git.hui.springai.app.service.AddressAdCodeService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author YiHui
 * @date 2025/7/11
 */
@RestController
public class ChatController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatController.class);

    private static final String SYSTEM_PROMPT = """
            你是一个专业的地址信息提取专家。请从用户输入的自然语言文本中提取结构化地址信息。
                    
            提取规则：
            1. 识别并分离出：省份、城市、区县、街道、详细地址
            2. 地址组件可能存在简称、别称，请转换为标准名称
            3. 如果用户输入包含"省"、"市"、"区"、"县"等关键词，需正确处理
            4. 行政区域编码必须使用提供的工具 queryAdCode 进行获取
                    
            输出格式要求：
            - 省份：完整省份名称，如"广东省"
            - 城市：地级市名称，直辖市填"北京市"等
            - 区县：区或县级市名称
            - 街道：街道、乡镇名称
            - 详细地址：门牌号、小区、楼栋等
            - 行政区域编码：通常是区县一级的编码，6位数字
                    
            示例输入："礼盒20个吉林省长春市朝阳区开运街领秀朝阳小区11栋2号楼304 田甜 18692093383"
            示例输出： {
                "province": "吉林省",
                "city": "长春市",
                "area": "朝阳区",
                "street": "开运街领秀朝阳小区",
                "detailInfo": "11栋2号楼304",
                "adCode": "220104",
                "personName": "田甜",
                "personPhone": "18692093383"
            }
            """;

    private final ChatModel chatModel;

    private final ChatClient chatClient;

    private final AddressAdCodeService addressAdCodeService;

    @Autowired
    public ChatController(ChatModel chatModel, AddressAdCodeService addressAdCodeService) {
        this.chatModel = chatModel;
        this.addressAdCodeService = addressAdCodeService;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor(ModelOptionsUtils::toJsonStringPrettyPrinter, ModelOptionsUtils::toJsonStringPrettyPrinter, 0))
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    /**
     * 从传入的自然语言中，提取出地址信息
     *
     * @param content
     * @return
     */
    @GetMapping("/ai/genAddress")
    public Address generateAddress(String content) {
        BeanOutputConverter<Address> beanOutputConverter = new BeanOutputConverter<>(Address.class);
        String format = beanOutputConverter.getFormat();


        PromptTemplate template = new PromptTemplate("请从下面个你的文本中，帮我提取详细的地址信息，要求中文返回: \n\n地址信息：\n{area} \n\n返回格式:{format}");
        Prompt prompt = template.create(Map.of("area", content, "format", format));
        Generation generation = chatModel.call(prompt).getResult();
        if (generation == null) {
            return null;
        }
        return beanOutputConverter.convert(generation.getOutput().getText());
    }


    @GetMapping("/ai/genAddressWithPromptTemplate")
    public Address generateAddressWithPromptTemplate(String content) {
        ChatClient.CallResponseSpec res = chatClient.prompt(content).call();
        Address address = res.entity(Address.class);
        return address;
    }

    /**
     * 从传入的自然语言中，提取出地址信息
     *
     * @param content
     * @return
     */
    @GetMapping("/ai/genAddressWithCodeTool")
    public Address generateAddressWithCodeTool(String content) {
        ChatClient.CallResponseSpec res = chatClient.prompt(content).tools(addressAdCodeService).call();
        Address address = res.entity(Address.class);
        return address;
    }

    record Address(
            @JsonPropertyDescription("省，如 湖北省")
            String province,
            @JsonPropertyDescription("市，如 武汉市")
            String city,
            @JsonPropertyDescription("区，如 武昌区")
            String area,
            @JsonPropertyDescription("街道，如 东湖路")
            String street,
            @JsonPropertyDescription("行政区域编码，如 420106")
            String adCode,
            @JsonPropertyDescription("详细地址，如 发财无限公司8栋8单元888号")
            String detailInfo,
            @JsonPropertyDescription("联系人，如 张三")
            String personName,
            @JsonPropertyDescription("联系人电话，如 15345785872")
            String personPhone
    ) {
    }

}