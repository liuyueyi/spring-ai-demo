package com.git.hui.springai.ali.express;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.git.hui.springai.ali.express.tools.ExpressOrderTools;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;

/**
 * 快递下单流程的 Hook，提供基于步骤的配置：
 * - 定义每个步骤的系统提示词
 * - 配置每个步骤可用的工具
 * - 定义每个步骤所需的前置条件
 * 
 * @author YiHui
 * @date 2026/3/12
 */
public class HandoffExpressOrderHook extends ModelHook {

    private static final String RECEIVE_INFO_COLLECTOR_PROMPT = """
            你是一名快递下单助手，负责协助用户完成快递下单。
                        
            CURRENT STEP: receive_info_collector
            ORDER STEP: 1/4
                        
            在这一步，你需要：
            1. 热情地迎接用户
            2. 询问收件人的详细信息（姓名、电话、详细地址）
            3. 用户提供信息后，必须调用 receiveAddress 工具保存收件人信息，然后自动进入下一步
                       
            保持对话式且友好。不要一次问太多问题。如果用户提供的信息不完整，请引导补充。
            """;

    private static final String SEND_INFO_COLLECTOR_PROMPT = """
            你是一名快递下单助手，负责协助用户完成快递下单。
                        
            CURRENT STEP: send_info_collector
            ORDER STEP: 2/4
            COLLECTED INFO: 收件人信息已收集
                        
            在这一步，你需要：
            1. 确认收件人信息已收集
            2. 请用户提供发件人的详细信息（姓名、电话、详细地址）
            3. 用户提供信息后，必须调用 sendAddress 工具记录，并自动进入下一步
            
            如有不清楚之处，请在收集前提出澄清问题。
            """;

    private static final String EXPRESS_INFO_COLLECTOR_PROMPT = """
            你是一名快递下单助手，负责协助用户完成快递下单。
                        
            CURRENT STEP: express_info_collector
            ORDER STEP: 3/4
            COLLECTED INFO: 收件人信息、发件人信息已收集
                        
            在这一步，你需要：
            1. 确认发件人和收件人信息已收集
            2. 请用户描述要寄送的物品（如：五本书、一件衣服等）
            3. 用户提供信息后，必须调用 expressInfo 工具记录，并自动进入下一步
            
            可以询问物品的类型、数量、重量等信息，以便更好地服务。
            """;

    private static final String ORDER_CONFIRMATION_PROMPT = """
            你是一名快递下单助手，负责协助用户完成快递下单。
                        
            CURRENT STEP: order_confirmation
            ORDER STEP: 4/4
            COLLECTED INFO: 收件人信息、发件人信息、物品信息已全部收集
            REQUIRED ACTION: 调用 showExpressOrder 显示订单详情，等待用户确认后调用 createOrder 创建订单
            """;

    private final ModelInterceptor stepConfigInterceptor;

    public HandoffExpressOrderHook() {
        ToolCallback receiveAddress = ExpressOrderTools.findByName("receiveAddress");
        ToolCallback sendAddress = ExpressOrderTools.findByName("sendAddress");
        ToolCallback expressInfo = ExpressOrderTools.findByName("expressInfo");
        ToolCallback showExpressOrder = ExpressOrderTools.findByName("showExpressOrder");
        ToolCallback createOrder = ExpressOrderTools.findByName("createOrder");


        this.stepConfigInterceptor = new StepConfigInterceptor(Map.of(
                ExpressOrderStateConstants.STEP_RECEIVE_INFO_COLLECTOR, new StepConfigInterceptor.StepConfig(RECEIVE_INFO_COLLECTOR_PROMPT, List.of(receiveAddress), List.of()),
                ExpressOrderStateConstants.STEP_SEND_INFO_COLLECTOR, new StepConfigInterceptor.StepConfig(SEND_INFO_COLLECTOR_PROMPT, List.of(sendAddress), List.of(ExpressOrderStateConstants.KEY_RECEIVE_INFO)),
                ExpressOrderStateConstants.STEP_EXPRESS_INFO_COLLECTOR, new StepConfigInterceptor.StepConfig(EXPRESS_INFO_COLLECTOR_PROMPT, List.of(expressInfo), List.of(ExpressOrderStateConstants.KEY_RECEIVE_INFO, ExpressOrderStateConstants.KEY_SEND_INFO)),
                ExpressOrderStateConstants.STEP_ORDER_CONFIRMATION, new StepConfigInterceptor.StepConfig(ORDER_CONFIRMATION_PROMPT, List.of(showExpressOrder, createOrder), List.of(ExpressOrderStateConstants.KEY_RECEIVE_INFO, ExpressOrderStateConstants.KEY_SEND_INFO, ExpressOrderStateConstants.KEY_EXPRESS_INFO))
        ));
    }

    @Override
    public String getName() {
        return "HandoffsExpressOrder";
    }

    @Override
    public List<ModelInterceptor> getModelInterceptors() {
        return List.of(stepConfigInterceptor);
    }

    @Override
    public Map<String, KeyStrategy> getKeyStrategys() {
        return Map.of(
                ExpressOrderStateConstants.KEY_ORDER_STEP, new ReplaceStrategy(),
                ExpressOrderStateConstants.KEY_CURRENT_STEP, new ReplaceStrategy(),
                ExpressOrderStateConstants.KEY_RECEIVE_INFO, new ReplaceStrategy(),
                ExpressOrderStateConstants.KEY_SEND_INFO, new ReplaceStrategy(),
                ExpressOrderStateConstants.KEY_EXPRESS_INFO, new ReplaceStrategy()
        );
    }
}
