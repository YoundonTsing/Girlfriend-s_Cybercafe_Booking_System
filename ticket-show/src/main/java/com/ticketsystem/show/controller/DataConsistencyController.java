package com.ticketsystem.show.controller;

import com.ticketsystem.common.result.Result;
import com.ticketsystem.show.service.DataConsistencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 数据一致性管理控制器
 */
@RestController
@RequestMapping("/api/consistency")
@RequiredArgsConstructor
@Tag(name = "数据一致性管理", description = "Redis与数据库数据一致性检查和修复")
@Slf4j
public class DataConsistencyController {

    private final DataConsistencyService dataConsistencyService;

    @GetMapping("/stock/check/{ticketId}")
    @Operation(summary = "检查指定票档库存一致性")
    public Result<Boolean> checkStockConsistency(@PathVariable Long ticketId) {
        log.info("检查票档库存一致性，票档ID：{}", ticketId);
        Boolean isConsistent = dataConsistencyService.checkStockConsistency(ticketId);
        return Result.success(isConsistent);
    }

    @PostMapping("/stock/repair/{ticketId}")
    @Operation(summary = "修复指定票档库存一致性")
    public Result<Boolean> repairStockConsistency(@PathVariable Long ticketId) {
        log.info("修复票档库存一致性，票档ID：{}", ticketId);
        Boolean repairResult = dataConsistencyService.repairStockConsistency(ticketId);
        return Result.success(repairResult);
    }

    @GetMapping("/stock/check-all")
    @Operation(summary = "批量检查所有票档库存一致性")
    public Result<Integer> checkAllStockConsistency() {
        log.info("批量检查所有票档库存一致性");
        Integer inconsistentCount = dataConsistencyService.checkAllStockConsistency();
        return Result.success(inconsistentCount);
    }

    @PostMapping("/stock/repair-all")
    @Operation(summary = "批量修复所有票档库存一致性")
    public Result<Integer> repairAllStockConsistency() {
        log.info("批量修复所有票档库存一致性");
        Integer repairedCount = dataConsistencyService.repairAllStockConsistency();
        return Result.success(repairedCount);
    }
}