package com.git.hui.springai.ali.express;

/**
 * 快递下单流程的状态常量定义
 * 
 * @author YiHui
 * @date 2026/3/12
 */
public class ExpressOrderStateConstants {

    // ==================== 状态键常量 ====================
    
    /** 当前步骤 */
    public static final String KEY_ORDER_STEP = "order_step";
    public static final String KEY_CURRENT_STEP = "current_step";

    /** 收件人信息 */
    public static final String KEY_RECEIVE_INFO = "receive_info";
    
    /** 发件人信息 */
    public static final String KEY_SEND_INFO = "send_info";
    
    /** 快递物品信息 */
    public static final String KEY_EXPRESS_INFO = "express_info";

    // ==================== 步骤常量 ====================
    
    /** 步骤 1: 收件人信息收集 */
    public static final String STEP_RECEIVE_INFO_COLLECTOR = "receive_info_collector";
    
    /** 步骤 2: 发件人信息收集 */
    public static final String STEP_SEND_INFO_COLLECTOR = "send_info_collector";
    
    /** 步骤 3: 快递物品信息收集 */
    public static final String STEP_EXPRESS_INFO_COLLECTOR = "express_info_collector";
    
    /** 步骤 4: 订单确认与创建 */
    public static final String STEP_ORDER_CONFIRMATION = "order_confirmation";

    private ExpressOrderStateConstants() {
        // 防止实例化
    }
}
