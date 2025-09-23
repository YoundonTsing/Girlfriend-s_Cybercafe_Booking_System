package com.ticketsystem.show.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticketsystem.show.dto.ShowInfoDTO;
import com.ticketsystem.show.entity.Show;
import com.ticketsystem.show.vo.ShowDetailVO;
import com.ticketsystem.show.vo.ShowVO;

import java.util.List;

public interface ShowService {

    /**
     * 分页查询演出列表
     * @param page 页码
     * @param size 每页大小
     * @param type 演出类型
     * @param city 城市
     * @param keyword 关键词
     * @return 演出列表
     */
    Page<ShowVO> pageShows(Integer page, Integer size, Integer type, String city, String keyword);

    /**
     * 获取热门演出列表
     * @return 热门演出列表
     */
    List<ShowVO> getHotShows();

    /**
     * 获取推荐演出列表
     * @return 推荐演出列表
     */
    List<ShowVO> getRecommendShows();

    /**
     * 获取演出详情
     * @param id 演出ID
     * @return 演出详情
     */
    ShowDetailVO getShowDetail(Long id);

    /**
     * 创建演出
     * @param show 演出信息
     * @return 演出ID
     */
    Long createShow(Show show);

    /**
     * 更新演出信息
     * @param show 演出信息
     */
    void updateShow(Show show);

    /**
     * 删除演出
     * @param id 演出ID
     */
    void deleteShow(Long id);

    /**
     * 获取演出信息（用于订单服务调用）
     * @param showId 演出ID
     * @param sessionId 场次ID
     * @return 演出信息
     */
    ShowInfoDTO getShowInfo(Long showId, Long sessionId);
}