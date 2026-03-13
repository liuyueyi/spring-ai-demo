package com.git.hui.springai.ali.express.tools;

import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import com.git.hui.springai.ali.express.ExpressOrderStateConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Arrays;
import java.util.List;

import static com.git.hui.springai.ali.express.ExpressOrderStateConstants.KEY_CURRENT_STEP;
import static com.git.hui.springai.ali.express.ExpressOrderStateConstants.KEY_EXPRESS_INFO;
import static com.git.hui.springai.ali.express.ExpressOrderStateConstants.KEY_ORDER_STEP;
import static com.git.hui.springai.ali.express.ExpressOrderStateConstants.KEY_RECEIVE_INFO;
import static com.git.hui.springai.ali.express.ExpressOrderStateConstants.KEY_SEND_INFO;


/**
 * 快递下单工具类
 * 通过多轮对话收集收件人信息、发件人信息、快递物品信息，最终完成下单
 * @author YiHui
 * @date 2026/3/12
 */
@Slf4j
public class ExpressOrderTools {
    private static final ExpressOrderTools INSTANCE = new ExpressOrderTools();

    private static final List<ToolCallback> TOOLS = Arrays.asList(ToolCallbacks.from(INSTANCE));

    /**
     * 获取所有工具回调
     */
    public static List<ToolCallback> getTOOLS() {
        return TOOLS;
    }

    /**
     * 根据名称查找工具回调（包内可见）
     */
    public static ToolCallback findByName(String name) {
        return TOOLS.stream()
                .filter(t -> name.equals(t.getToolDefinition().name()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown tool: " + name));
    }

    /**
     * 第一步：接收并保存收件人信息
     */
    @Tool(description = "保存快递下单的收件人信息，用于快递下单场景")
    public String receiveAddress(
            @ToolParam(description = "收件人详细信息，包括姓名、电话、地址等") String receiveInfo,
            ToolContext toolContext) {
        log.info("【快递下单】收件人信息：{}", receiveInfo);

        // 保存到上下文中
        saveToContext(toolContext, KEY_RECEIVE_INFO, receiveInfo);
        updateStep(toolContext, 2); // 更新到第 2 步

        return "✅ 收件人信息已保存：" + receiveInfo + "\n\n请提供发件人信息（姓名、电话、地址）";
    }

    /**
     * 第二步：接收并保存发件人信息
     */
    @Tool(description = "保存快递下单的发货人信息，用于快递下单场景")
    public String sendAddress(
            @ToolParam(description = "发件人详细信息，包括姓名、电话、地址等") String sendInfo,
            ToolContext toolContext) {
        log.info("【快递下单】发件人信息：{}", sendInfo);

        // 保存到上下文中
        saveToContext(toolContext, KEY_SEND_INFO, sendInfo);
        updateStep(toolContext, 3); // 更新到第 3 步

        return "✅ 发件人信息已保存：" + sendInfo + "\n\n请描述要寄送的物品（如：五本书、一件衣服等）";
    }

    /**
     * 第三步：接收并保存快递物品信息
     */
    @Tool(description = "保存快递下单的寄送物品信息，如物品重量、体积、特殊要求、类型等，用于快递下单场景")
    public String expressInfo(
            @ToolParam(description = "寄送的快递信息，如 五本书、一件衣服等") String expressInfo,
            ToolContext toolContext
    ) {
        log.info("【快递下单】快递物品信息：{}", expressInfo);

        // 保存到上下文中
        saveToContext(toolContext, KEY_EXPRESS_INFO, expressInfo);
        updateStep(toolContext, 4); // 更新到第 4 步

        return "✅ 快递物品信息已保存：" + expressInfo + "\n\n所有信息已收集完毕！可以调用 showExpressOrder 查看现在的订单信息";
    }

    /**
     * 第四步：显示当前订单信息
     */
    @Tool(description = "当用户希望查看订单信息时，调用这个工具返回订单信息")
    public String showExpressOrder(ToolContext toolContext) {
        log.info("【快递下单】显示订单信息");

        String receiveInfo = getFromContext(toolContext, KEY_RECEIVE_INFO);
        String sendInfo = getFromContext(toolContext, KEY_SEND_INFO);
        String expressInfo = getFromContext(toolContext, KEY_EXPRESS_INFO);

        StringBuilder sb = new StringBuilder("📦 当前订单信息:\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("📮 收件人：").append(receiveInfo != null ? receiveInfo : "未填写").append("\n");
        sb.append("📤 发件人：").append(sendInfo != null ? sendInfo : "未填写").append("\n");
        sb.append("📦 物\u54c1：").append(expressInfo != null ? expressInfo : "未填写").append("\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");

        var step = getFromContext(toolContext, KEY_ORDER_STEP);
        sb.append("\n当前进度：第 ").append(step).append("/4 步\n");

        var intStep = Integer.parseInt(step);
        if (intStep < 4) {
            sb.append("\n⚠️ 信息未填写完整，请继续补充");
            if (receiveInfo == null) {
                sb.append("\n→ 请先提供收件人信息");
            } else if (sendInfo == null) {
                sb.append("\n→ 请提供发件人信息");
            } else if (expressInfo == null) {
                sb.append("\n→ 请描述要寄送的物品");
            }
        } else {
            sb.append("\n✅ 所有信息已齐全，可以下单了！");
        }

        return sb.toString();
    }

    /**
     * 第五步：创建订单（模拟）
     */
    @Tool(description = "当用户确认创建订单时，调用这个工具执行创建订单动作")
    public String createOrder(ToolContext toolContext) {
        log.info("【快递下单】创建订单");

        String receiveInfo = getFromContext(toolContext, KEY_RECEIVE_INFO);
        String sendInfo = getFromContext(toolContext, KEY_SEND_INFO);
        String expressInfo = getFromContext(toolContext, KEY_EXPRESS_INFO);

        // 检查信息是否完整
        if (receiveInfo == null || sendInfo == null || expressInfo == null) {
            return "❌ 订单信息不完整，无法下单！\n\n" + showExpressOrder(toolContext);
        }

        // 模拟生成订单号
        String orderNo = "EXP" + System.currentTimeMillis();

        StringBuilder sb = new StringBuilder("🎉 下单成功！\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("📝 订单号：").append(orderNo).append("\n");
        sb.append("📮 收件人：").append(receiveInfo).append("\n");
        sb.append("📤 发件人：").append(sendInfo).append("\n");
        sb.append("📦 物\u54c1：").append(expressInfo).append("\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("\n✅ 快递员将尽快上门取件，请耐心等待");

        // 下单完毕之后，如何进行上下文信息的重置？
        ToolContextHelper.getStateForUpdate(toolContext).ifPresent(state -> {
            state.put(KEY_CURRENT_STEP, ExpressOrderStateConstants.STEP_RECEIVE_INFO_COLLECTOR);
            state.put(KEY_ORDER_STEP, 1);
            state.put(KEY_RECEIVE_INFO, "");
            state.put(KEY_SEND_INFO, "");
            state.put(KEY_EXPRESS_INFO, "");
        });
        return sb.toString();
    }

    /**
     * 从上下文中获取数据
     */
    @SuppressWarnings("unchecked")
    private String getFromContext(ToolContext toolContext, String key) {
        return String.valueOf(ToolContextHelper.getState(toolContext).get().data().get(key));
    }

    /**
     * 保存数据到上下文
     */
    @SuppressWarnings("unchecked")
    private void saveToContext(ToolContext toolContext, String key, String value) {
        ToolContextHelper.getStateForUpdate(toolContext).ifPresent(up -> up.put(key, value));
        log.info("保存数据到上下文：{} = {}", key, value);
    }

    /**
     * 更新当前步骤
     */
    @SuppressWarnings("unchecked")
    private void updateStep(ToolContext toolContext, int step) {
        ToolContextHelper.getStateForUpdate(toolContext).ifPresent(context -> {
            context.put(KEY_ORDER_STEP, step);
            switch (step) {
                case 0:
                    // 重置进度
                    context.clear();
                    break;
                case 2:
                    context.put(KEY_CURRENT_STEP, ExpressOrderStateConstants.STEP_SEND_INFO_COLLECTOR);
                    break;
                case 3:
                    context.put(KEY_CURRENT_STEP, ExpressOrderStateConstants.STEP_EXPRESS_INFO_COLLECTOR);
                    break;
                case 4:
                    context.put(KEY_CURRENT_STEP, ExpressOrderStateConstants.STEP_ORDER_CONFIRMATION);
            }

            log.info("更新订单步骤到：{} - {}", step, context.get(KEY_CURRENT_STEP));
        });
    }
}
