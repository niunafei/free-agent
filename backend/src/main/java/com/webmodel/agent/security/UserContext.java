package com.webmodel.agent.security;

public class UserContext {
    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        CURRENT.set(userId);
    }

    public static Long getUserId() {
        return CURRENT.get();
    }

    public static void setRole(String role) {
        ROLE.set(role);
    }

    public static String getRole() {
        return ROLE.get();
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(ROLE.get());
    }

    public static void clear() {
        CURRENT.remove();
        ROLE.remove();
    }
}