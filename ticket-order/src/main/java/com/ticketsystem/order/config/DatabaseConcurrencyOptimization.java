package com.ticketsystem.order.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 数据库并发能力优化配置
 * 解决数据库成为性能瓶颈的问题
 */
@Configuration
@EnableAsync
@EnableScheduling
public class DatabaseConcurrencyOptimization {

    private final AtomicLong writeConnectionCount = new AtomicLong(0);
    private final AtomicLong readConnectionCount = new AtomicLong(0);
    
    @Value("${DB_HOST:localhost}")
    private String dbHost;
    
    @Value("${DB_PORT:3306}")
    private String dbPort;
    
    @Value("${DB_NAME:ticket_order_db}")
    private String dbName;
    
    @Value("${DB_USERNAME:root}")
    private String dbUsername;
    
    @Value("${DB_PASSWORD:123456}")
    private String dbPassword;

    /**
     * 高并发写数据源 - 针对订单创建、支付等写操作优化
     */
    @Bean(name = "highConcurrencyWriteDataSource")
    @Primary
    public DataSource highConcurrencyWriteDataSource() {
        HikariConfig config = new HikariConfig();
        
        // 连接池配置 - 针对高并发优化
        config.setMaximumPoolSize(100);  // 增加最大连接数
        config.setMinimumIdle(20);       // 增加最小空闲连接
        config.setConnectionTimeout(10000);  // 减少连接超时时间
        config.setIdleTimeout(300000);   // 5分钟空闲超时
        config.setMaxLifetime(1800000);  // 30分钟最大生命周期
        config.setLeakDetectionThreshold(30000);  // 30秒泄漏检测
        
        // 连接池名称
        config.setPoolName("HighConcurrencyWritePool");
        
        // 数据库连接URL优化
        String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?" +
                "useUnicode=true&" +
                "characterEncoding=utf8&" +
                "useSSL=false&" +
                "serverTimezone=Asia/Shanghai&" +
                "allowMultiQueries=true&" +
                "rewriteBatchedStatements=true&" +  // 批量重写
                "useServerPrepStmts=true&" +        // 服务器预编译
                "cachePrepStmts=true&" +            // 缓存预编译语句
                "prepStmtCacheSize=500&" +          // 预编译语句缓存大小
                "prepStmtCacheSqlLimit=2048&" +     // 预编译语句缓存SQL限制
                "useLocalSessionState=true&" +      // 使用本地会话状态
                "elideSetAutoCommits=true&" +       // 隐藏自动提交
                "maintainTimeStats=false&" +        // 不维护时间统计
                "autoReconnect=true&" +             // 自动重连
                "failOverReadOnly=false&" +         // 故障转移只读
                "maxReconnects=3&" +                // 最大重连次数
                "initialTimeout=1&" +               // 初始超时
                "connectTimeout=10000&" +           // 连接超时
                "socketTimeout=30000",              // Socket超时
                dbHost, dbPort, dbName);
        
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // 连接池性能优化
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "500");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        // 事务隔离级别优化
        config.addDataSourceProperty("defaultTransactionIsolation", "READ_COMMITTED");
        
        // 连接验证
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(3000);
        
        return new HikariDataSource(config);
    }

    /**
     * 高并发读数据源 - 针对订单查询等读操作优化
     */
    @Bean(name = "highConcurrencyReadDataSource")
    public DataSource highConcurrencyReadDataSource() {
        HikariConfig config = new HikariConfig();
        
        // 连接池配置 - 针对高并发读操作优化
        config.setMaximumPoolSize(200);  // 读操作可以更多连接
        config.setMinimumIdle(50);       // 更多空闲连接
        config.setConnectionTimeout(5000);   // 更短的连接超时
        config.setIdleTimeout(180000);   // 3分钟空闲超时
        config.setMaxLifetime(1200000);  // 20分钟最大生命周期
        config.setLeakDetectionThreshold(30000);
        
        // 连接池名称
        config.setPoolName("HighConcurrencyReadPool");
        
        // 数据库连接URL优化 - 读库专用
        String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?" +
                "useUnicode=true&" +
                "characterEncoding=utf8&" +
                "useSSL=false&" +
                "serverTimezone=Asia/Shanghai&" +
                "allowMultiQueries=true&" +
                "rewriteBatchedStatements=true&" +
                "useServerPrepStmts=true&" +
                "cachePrepStmts=true&" +
                "prepStmtCacheSize=1000&" +         // 读操作缓存更多
                "prepStmtCacheSqlLimit=4096&" +     // 更大的SQL限制
                "useLocalSessionState=true&" +
                "elideSetAutoCommits=true&" +
                "maintainTimeStats=false&" +
                "autoReconnect=true&" +
                "failOverReadOnly=true&" +          // 读库故障转移只读
                "maxReconnects=3&" +
                "initialTimeout=1&" +
                "connectTimeout=5000&" +            // 更短的连接超时
                "socketTimeout=15000&" +            // 更短的Socket超时
                "useReadOnly=true",                 // 明确标记为只读
                dbHost, dbPort, dbName);
        
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // 读库性能优化
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "1000");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "4096");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        // 读库优化配置
        config.addDataSourceProperty("defaultTransactionIsolation", "READ_UNCOMMITTED");
        config.addDataSourceProperty("useReadOnly", "true");
        
        // 连接验证
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(2000);
        
        return new HikariDataSource(config);
    }

    /**
     * 高并发写JdbcTemplate
     */
    @Bean(name = "highConcurrencyWriteJdbcTemplate")
    @Primary
    public JdbcTemplate highConcurrencyWriteJdbcTemplate(
            @Qualifier("highConcurrencyWriteDataSource") DataSource writeDataSource) {
        JdbcTemplate template = new JdbcTemplate(writeDataSource);
        
        // JdbcTemplate性能优化
        template.setFetchSize(1000);  // 增加获取大小
        template.setMaxRows(10000);   // 设置最大行数
        template.setQueryTimeout(30); // 30秒查询超时
        
        return template;
    }

    /**
     * 高并发读JdbcTemplate
     */
    @Bean(name = "highConcurrencyReadJdbcTemplate")
    public JdbcTemplate highConcurrencyReadJdbcTemplate(
            @Qualifier("highConcurrencyReadDataSource") DataSource readDataSource) {
        JdbcTemplate template = new JdbcTemplate(readDataSource);
        
        // 读操作JdbcTemplate优化
        template.setFetchSize(2000);  // 读操作可以获取更多
        template.setMaxRows(50000);   // 读操作可以返回更多行
        template.setQueryTimeout(60); // 读操作可以更长的超时
        
        return template;
    }

    /**
     * 异步任务执行器配置
     */
    @Bean(name = "orderAsyncExecutor")
    public Executor orderAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(10);
        
        // 最大线程数
        executor.setMaxPoolSize(50);
        
        // 队列容量
        executor.setQueueCapacity(200);
        
        // 线程空闲时间
        executor.setKeepAliveSeconds(60);
        
        // 线程名前缀
        executor.setThreadNamePrefix("OrderAsync-");
        
        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }

    /**
     * 数据库连接池监控器
     */
    @Bean
    public DatabaseConnectionPoolMonitor databaseConnectionPoolMonitor(
            @Qualifier("highConcurrencyWriteDataSource") DataSource writeDataSource,
            @Qualifier("highConcurrencyReadDataSource") DataSource readDataSource) {
        return new DatabaseConnectionPoolMonitor(writeDataSource, readDataSource);
    }

    /**
     * 定时监控数据库连接池状态
     */
    @Scheduled(fixedRate = 30000) // 每30秒监控一次
    public void monitorConnectionPools() {
        try {
            // 获取写库数据源
            DataSource writeDataSource = highConcurrencyWriteDataSource();
            if (writeDataSource instanceof HikariDataSource) {
                HikariDataSource writeDs = (HikariDataSource) writeDataSource;
                long active = writeDs.getHikariPoolMXBean().getActiveConnections();
                long idle = writeDs.getHikariPoolMXBean().getIdleConnections();
                long total = writeDs.getHikariPoolMXBean().getTotalConnections();
                long waiting = writeDs.getHikariPoolMXBean().getThreadsAwaitingConnection();
                
                writeConnectionCount.set(active);
                
                if (active > 80) { // 活跃连接超过80个时告警
                    System.out.println("⚠️ 写库连接池使用率过高: 活跃=" + active + ", 空闲=" + idle + ", 总连接=" + total + ", 等待=" + waiting);
                }
            }
            
            // 获取读库数据源
            DataSource readDataSource = highConcurrencyReadDataSource();
            if (readDataSource instanceof HikariDataSource) {
                HikariDataSource readDs = (HikariDataSource) readDataSource;
                long active = readDs.getHikariPoolMXBean().getActiveConnections();
                long idle = readDs.getHikariPoolMXBean().getIdleConnections();
                long total = readDs.getHikariPoolMXBean().getTotalConnections();
                long waiting = readDs.getHikariPoolMXBean().getThreadsAwaitingConnection();
                
                readConnectionCount.set(active);
                
                if (active > 150) { // 活跃连接超过150个时告警
                    System.out.println("⚠️ 读库连接池使用率过高: 活跃=" + active + ", 空闲=" + idle + ", 总连接=" + total + ", 等待=" + waiting);
                }
            }
            
        } catch (Exception e) {
            System.err.println("监控数据库连接池失败: " + e.getMessage());
        }
    }

    /**
     * 数据库连接池监控器
     */
    public static class DatabaseConnectionPoolMonitor {
        private final DataSource writeDataSource;
        private final DataSource readDataSource;

        public DatabaseConnectionPoolMonitor(DataSource writeDataSource, DataSource readDataSource) {
            this.writeDataSource = writeDataSource;
            this.readDataSource = readDataSource;
        }

        /**
         * 获取写库连接池状态
         */
        public String getWritePoolStatus() {
            if (writeDataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) writeDataSource;
                return String.format("写库连接池 - 活跃连接: %d, 空闲连接: %d, 总连接: %d, 等待连接: %d",
                        hikariDataSource.getHikariPoolMXBean().getActiveConnections(),
                        hikariDataSource.getHikariPoolMXBean().getIdleConnections(),
                        hikariDataSource.getHikariPoolMXBean().getTotalConnections(),
                        hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
            }
            return "写库连接池状态未知";
        }

        /**
         * 获取读库连接池状态
         */
        public String getReadPoolStatus() {
            if (readDataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) readDataSource;
                return String.format("读库连接池 - 活跃连接: %d, 空闲连接: %d, 总连接: %d, 等待连接: %d",
                        hikariDataSource.getHikariPoolMXBean().getActiveConnections(),
                        hikariDataSource.getHikariPoolMXBean().getIdleConnections(),
                        hikariDataSource.getHikariPoolMXBean().getTotalConnections(),
                        hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
            }
            return "读库连接池状态未知";
        }

        /**
         * 获取连接池健康状态
         */
        public boolean isHealthy() {
            try {
                if (writeDataSource instanceof HikariDataSource) {
                    HikariDataSource writeDs = (HikariDataSource) writeDataSource;
                    long writeActive = writeDs.getHikariPoolMXBean().getActiveConnections();
                    long writeTotal = writeDs.getHikariPoolMXBean().getTotalConnections();
                    
                    if (writeActive > writeTotal * 0.9) { // 写库使用率超过90%
                        return false;
                    }
                }
                
                if (readDataSource instanceof HikariDataSource) {
                    HikariDataSource readDs = (HikariDataSource) readDataSource;
                    long readActive = readDs.getHikariPoolMXBean().getActiveConnections();
                    long readTotal = readDs.getHikariPoolMXBean().getTotalConnections();
                    
                    if (readActive > readTotal * 0.9) { // 读库使用率超过90%
                        return false;
                    }
                }
                
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}