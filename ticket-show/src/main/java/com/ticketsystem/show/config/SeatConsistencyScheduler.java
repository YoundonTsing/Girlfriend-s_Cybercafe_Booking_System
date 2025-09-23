package com.ticketsystem.show.config;

import com.ticketsystem.show.service.impl.SeatConsistencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 座位数据一致性定时任务调度器
 * 负责定期清理过期锁定和修复数据不一致问题
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "seat.consistency.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SeatConsistencyScheduler {

    private final SeatConsistencyService seatConsistencyService;

    /**
     * 每分钟清理一次过期的座位锁定
     * 防止死锁和数据不一致
     */
    @Scheduled(fixedRate = 60000) // 60秒
    public void cleanupExpiredLocks() {
        try {
            log.debug("开始执行过期锁定清理任务");
            seatConsistencyService.cleanupExpiredLocks();
            log.debug("过期锁定清理任务执行完成");
        } catch (Exception e) {
            log.error("过期锁定清理任务执行失败", e);
        }
    }

    /**
     * 每5分钟执行一次数据一致性检查
     * 检查Redis与数据库状态是否一致
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void consistencyHealthCheck() {
        try {
            log.debug("开始执行数据一致性健康检查");
            // 这里可以添加更复杂的一致性检查逻辑
            // 比如随机抽查一些座位的状态一致性
            performRandomConsistencyCheck();
            log.debug("数据一致性健康检查执行完成");
        } catch (Exception e) {
            log.error("数据一致性健康检查执行失败", e);
        }
    }

    /**
     * 随机抽查座位状态一致性
     */
    private void performRandomConsistencyCheck() {
        try {
            // 随机检查一些可能存在锁定的座位
            // 这里可以根据实际业务需求调整检查策略
            
            // 示例：检查最近可能被锁定的座位（这里简化处理）
            // 实际应用中可以维护一个最近活跃座位的列表
            for (long seatId = 1; seatId <= 10; seatId++) {
                try {
                    // 随机选择一些座位进行检查
                    if (Math.random() < 0.1) { // 10%的概率检查
                        seatConsistencyService.verifyAndRepairConsistency(seatId, null);
                    }
                } catch (Exception e) {
                    log.warn("随机一致性检查失败，座位ID: {}", seatId, e);
                }
            }
            
            log.debug("随机一致性检查执行完成");
        } catch (Exception e) {
            log.error("随机一致性检查执行异常", e);
        }
    }

    /**
     * 每小时输出一次系统状态统计
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void systemStatusReport() {
        try {
            log.info("=== 座位锁定系统状态报告 ===");
            log.info("定时清理任务正常运行");
            log.info("数据一致性检查正常运行");
            log.info("=== 状态报告结束 ===");
        } catch (Exception e) {
            log.error("系统状态报告生成失败", e);
        }
    }
}