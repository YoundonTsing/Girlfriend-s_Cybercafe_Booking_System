package com.ticketsystem.show.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticketsystem.common.result.Result;
import com.ticketsystem.show.dto.ShowInfoDTO;
import com.ticketsystem.show.entity.Show;
import com.ticketsystem.show.service.ShowService;
import com.ticketsystem.show.vo.ShowDetailVO;
import com.ticketsystem.show.vo.ShowVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/show")
@RequiredArgsConstructor
@Tag(name = "演出管理", description = "演出相关接口")
public class ShowController {

    private final ShowService showService;

    @GetMapping("/list")
    @Operation(summary = "查询演出列表")
    public Result<Page<ShowVO>> listShows(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String keyword) {
        Page<ShowVO> result = showService.pageShows(page, size, type, city, keyword);
        return Result.success(result);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询演出列表")
    public Result<Page<ShowVO>> pageShows(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String keyword) {
        Page<ShowVO> result = showService.pageShows(page, size, type, city, keyword);
        return Result.success(result);
    }

    @GetMapping("/hot")
    @Operation(summary = "获取热门演出列表")
    public Result<List<ShowVO>> getHotShows() {
        List<ShowVO> hotShows = showService.getHotShows();
        return Result.success(hotShows);
    }

    @GetMapping("/recommend")
    @Operation(summary = "获取推荐演出列表")
    public Result<List<ShowVO>> getRecommendShows() {
        List<ShowVO> recommendShows = showService.getRecommendShows();
        return Result.success(recommendShows);
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "获取演出详情")
    public Result<ShowDetailVO> getShowDetail(@PathVariable Long id) {
        ShowDetailVO showDetail = showService.getShowDetail(id);
        return Result.success(showDetail);
    }

    @GetMapping("/info")
    @Operation(summary = "获取演出信息（用于订单服务调用）")
    public Result<ShowInfoDTO> getShowInfo(@RequestParam Long showId, @RequestParam Long sessionId) {
        ShowInfoDTO showInfo = showService.getShowInfo(showId, sessionId);
        return Result.success(showInfo);
    }

    @PostMapping
    @Operation(summary = "创建演出")
    public Result<Long> createShow(@RequestBody Show show) {
        Long showId = showService.createShow(show);
        return Result.success(showId);
    }

    @PutMapping
    @Operation(summary = "更新演出信息")
    public Result<Void> updateShow(@RequestBody Show show) {
        showService.updateShow(show);
        return Result.success();
    }

    @DeleteMapping("/detail/{id}")
    @Operation(summary = "删除演出")
    public Result<Void> deleteShow(@PathVariable Long id) {
        showService.deleteShow(id);
        return Result.success();
    }
}