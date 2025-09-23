package com.ticketsystem.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT工具类 - 与用户服务保持一致
 */
@Slf4j
public class JwtUtil {

    // 密钥 - 与用户服务保持一致
    private static final String SECRET = "ticketsystem123456789ticketsystem123456789";

    /**
     * 解析token
     * @param token token
     * @return Claims
     */
    public static Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 获取用户ID
     * @param token token
     * @return 用户ID
     */
    public static Long getUserId(String token) {
        try {
            Claims claims = parseToken(token);
            Object userIdObj = claims.get("userId");
            if (userIdObj != null) {
                return Long.valueOf(userIdObj.toString());
            }
            return null;
        } catch (Exception e) {
            log.error("获取用户ID异常", e);
            return null;
        }
    }

    /**
     * 获取用户名
     * @param token token
     * @return 用户名
     */
    public static String getUsername(String token) {
        try {
            Claims claims = parseToken(token);
            Object usernameObj = claims.get("username");
            if (usernameObj != null) {
                return usernameObj.toString();
            }
            return null;
        } catch (Exception e) {
            log.error("获取用户名异常", e);
            return null;
        }
    }

    /**
     * 验证token是否过期
     * @param token token
     * @return 是否过期
     */
    public static boolean isExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.error("验证token是否过期异常", e);
            return true;
        }
    }

    /**
     * 验证token是否有效
     * @param token token
     * @return 是否有效
     */
    public static boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.error("验证token异常", e);
            return false;
        }
    }
}