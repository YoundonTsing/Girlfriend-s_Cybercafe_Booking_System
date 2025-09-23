package com.ticketsystem.common.util;

/**
 * 用户上下文工具类，用于存储和获取当前用户ID
 */
public class UserContext {
    
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    
    /**
     * 设置用户ID
     * @param userId 用户ID
     */
    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }
    
    /**
     * 获取用户ID
     * @return 用户ID
     */
    public static Long getUserId() {
        return USER_ID.get();
    }
    
    /**
     * 清除用户ID
     */
    public static void clear() {
        USER_ID.remove();
    }
}