package com.webmodel.agent.security;

public class UserContext {
    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        CURRENT.set(userId);
    }

    public static Long getUserId() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}