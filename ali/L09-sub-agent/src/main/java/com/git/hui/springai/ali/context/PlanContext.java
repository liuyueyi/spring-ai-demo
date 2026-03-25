package com.git.hui.springai.ali.context;

import reactor.core.publisher.FluxSink;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author YiHui
 * @date 2026/3/24
 */
public class PlanContext {
    private static InheritableThreadLocal<FluxSink> context = new InheritableThreadLocal<>();

    private static Map<String, FluxSink> sessionEmitter = new ConcurrentHashMap<>();

    public static void set(FluxSink sink) {
        context.set(sink);
    }

    public static FluxSink get() {
        return context.get();
    }

    public static void remove() {
        context.remove();
    }

    public static void setSessionEmitter(String sessionId, FluxSink sink) {
        sessionEmitter.put(sessionId, sink);
    }

    public static FluxSink getSessionEmitter(String sessionId) {
        return sessionEmitter.get(sessionId);
    }

    public static void removeSessionEmitter(String sessionId) {
        sessionEmitter.remove(sessionId);
    }
}
