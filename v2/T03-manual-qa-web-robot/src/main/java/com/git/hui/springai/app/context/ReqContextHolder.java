package com.git.hui.springai.app.context;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author YiHui
 * @date 2026/1/28
 */
public class ReqContextHolder {

    private static final ThreadLocal<ReqInfo> reqId = new InheritableThreadLocal<>();

    public static void setReqId(ReqInfo reqId) {
        ReqContextHolder.reqId.set(reqId);
    }

    public static ReqInfo getReqId() {
        return reqId.get();
    }

    public static void clear() {
        reqId.remove();
    }

    public record ReqInfo(String chatId, SseEmitter sse) {
    }
}
