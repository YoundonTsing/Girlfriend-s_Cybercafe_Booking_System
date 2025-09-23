package com.ticketsystem.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SQL 脚本验证工具
 * 用于验证 SQL 脚本的安全性
 */
@Component
public class SqlValidator {

    private static final Logger logger = LoggerFactory.getLogger(SqlValidator.class);

    // 危险 SQL 关键词
    private static final List<String> DANGEROUS_KEYWORDS = Arrays.asList(
        "DROP", "DELETE", "TRUNCATE", "ALTER", "GRANT", "REVOKE", "EXEC", "EXECUTE",
        "xp_", "sp_", "OPENROWSET", "OPENDATASOURCE", "BULK INSERT"
    );

    // 危险文件路径模式
    private static final Pattern DANGEROUS_PATH_PATTERN = Pattern.compile(
        ".*[\\\\/]\\.\\..*|.*\\.\\.[\\\\/].*|.*[\\\\/]etc[\\\\/].*|.*[\\\\/]var[\\\\/].*"
    );

    /**
     * 验证 SQL 脚本安全性
     * @param sql SQL 脚本内容
     * @return 是否安全
     */
    public boolean validateSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            logger.warn("SQL 脚本为空");
            return false;
        }

        String upperSql = sql.toUpperCase().trim();

        // 检查危险关键词
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (upperSql.contains(keyword)) {
                logger.error("检测到危险 SQL 关键词: {}", keyword);
                return false;
            }
        }

        // 检查危险文件路径
        if (DANGEROUS_PATH_PATTERN.matcher(sql).matches()) {
            logger.error("检测到危险文件路径");
            return false;
        }

        // 检查是否包含注释
        if (upperSql.contains("--") || upperSql.contains("/*")) {
            logger.warn("SQL 脚本包含注释，请谨慎执行");
        }

        logger.info("SQL 脚本验证通过");
        return true;
    }

    /**
     * 验证数据库名称安全性
     * @param dbName 数据库名称
     * @return 是否安全
     */
    public boolean validateDatabaseName(String dbName) {
        if (dbName == null || dbName.trim().isEmpty()) {
            return false;
        }

        // 只允许字母、数字、下划线
        return dbName.matches("^[a-zA-Z0-9_]+$");
    }

    /**
     * 验证表名安全性
     * @param tableName 表名
     * @return 是否安全
     */
    public boolean validateTableName(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return false;
        }

        // 只允许字母、数字、下划线
        return tableName.matches("^[a-zA-Z0-9_]+$");
    }
} 