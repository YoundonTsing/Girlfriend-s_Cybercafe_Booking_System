package com.ticketsystem.show.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ticketsystem.common.exception.BusinessException;
import com.ticketsystem.common.result.ResultCode;
import com.ticketsystem.show.dto.ShowInfoDTO;
import com.ticketsystem.show.entity.Show;
import com.ticketsystem.show.entity.ShowSession;
import com.ticketsystem.show.mapper.ShowMapper;
import com.ticketsystem.show.mapper.ShowSessionMapper;
import com.ticketsystem.show.service.ShowService;
import com.ticketsystem.show.vo.SessionVO;
import com.ticketsystem.show.vo.ShowDetailVO;
import com.ticketsystem.show.vo.ShowVO;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowServiceImpl extends ServiceImpl<ShowMapper, Show> implements ShowService {

    private final ShowSessionMapper showSessionMapper;

    private static final Map<Integer, String> SHOW_TYPE_MAP = Map.of(
            1, "演唱会",
            2, "话剧",
            3, "音乐会",
            4, "展览",
            5, "体育赛事"
    );

    private static final Map<Integer, String> SHOW_STATUS_MAP = Map.of(
            0, "未开售",
            1, "售票中",
            2, "已售罄",
            3, "已结束"
    );

    @Override
    public Page<ShowVO> pageShows(Integer page, Integer size, Integer type, String city, String keyword) {
        Page<Show> showPage = new Page<>(page, size);
        
        LambdaQueryWrapper<Show> queryWrapper = new LambdaQueryWrapper<>();
        // 添加查询条件
        if (type != null) {
            queryWrapper.eq(Show::getType, type);
        }
        if (StringUtils.hasText(city)) {
            queryWrapper.eq(Show::getCity, city);
        }
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(Show::getName, keyword)
                    .or()
                    .like(Show::getDescription, keyword);
        }
        
        // 按开始时间降序排序
        queryWrapper.orderByDesc(Show::getStartTime);
        
        // 执行分页查询
        Page<Show> result = page(showPage, queryWrapper);
        
        // 转换为VO
        Page<ShowVO> voPage = new Page<>();
        BeanUtils.copyProperties(result, voPage, "records");
        
        List<ShowVO> voList = result.getRecords().stream()
                .map(this::convertToShowVO)
                .collect(Collectors.toList());
        
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public List<ShowVO> getHotShows() {
        LambdaQueryWrapper<Show> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Show::getIsHot, 1)
                .eq(Show::getStatus, 1)  // 售票中
                .orderByDesc(Show::getStartTime)
                .last("LIMIT 10");
        
        List<Show> shows = list(queryWrapper);
        return shows.stream()
                .map(this::convertToShowVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ShowVO> getRecommendShows() {
        LambdaQueryWrapper<Show> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Show::getIsRecommend, 1)
                .in(Show::getStatus, 0, 1)  // 未开售或售票中
                .orderByDesc(Show::getStartTime)
                .last("LIMIT 10");
        
        List<Show> shows = list(queryWrapper);
        return shows.stream()
                .map(this::convertToShowVO)
                .collect(Collectors.toList());
    }

    @Override
    public ShowDetailVO getShowDetail(Long id) {
        // 获取演出信息
        Show show = getById(id);
        if (show == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST.getCode(), "演出不存在");
        }
        
        ShowDetailVO detailVO = new ShowDetailVO();
        BeanUtils.copyProperties(show, detailVO);
        
        // 设置类型名称和状态名称
        detailVO.setTypeName(SHOW_TYPE_MAP.getOrDefault(show.getType(), "未知"));
        detailVO.setStatusName(SHOW_STATUS_MAP.getOrDefault(show.getStatus(), "未知"));
        
        // 获取场次信息
        LambdaQueryWrapper<ShowSession> sessionQueryWrapper = new LambdaQueryWrapper<>();
        sessionQueryWrapper.eq(ShowSession::getShowId, id)
                .orderByAsc(ShowSession::getStartTime);
        List<ShowSession> sessions = showSessionMapper.selectList(sessionQueryWrapper);
        
        List<SessionVO> sessionVOs = sessions.stream().map(session -> {
            SessionVO sessionVO = new SessionVO();
            BeanUtils.copyProperties(session, sessionVO);
            sessionVO.setStatusName(SHOW_STATUS_MAP.getOrDefault(session.getStatus(), "未知"));
            sessionVO.setRemainSeats(session.getTotalSeats() - session.getSoldSeats());
            return sessionVO;
        }).collect(Collectors.toList());
        
        detailVO.setSessions(sessionVOs);
        
        return detailVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createShow(Show show) {
        save(show);
        return show.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShow(Show show) {
        Show existShow = getById(show.getId());
        if (existShow == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST.getCode(), "演出不存在");
        }
        
        updateById(show);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteShow(Long id) {
        Show show = getById(id);
        if (show == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST.getCode(), "演出不存在");
        }
        
        // 检查是否有关联的场次
        LambdaQueryWrapper<ShowSession> sessionQueryWrapper = new LambdaQueryWrapper<>();
        sessionQueryWrapper.eq(ShowSession::getShowId, id);
        long sessionCount = showSessionMapper.selectCount(sessionQueryWrapper);
        
        if (sessionCount > 0) {
            throw new BusinessException(ResultCode.OPERATION_FAILED.getCode(), "演出已关联场次，无法删除");
        }
        
        removeById(id);
    }
    
    @Override
    public ShowInfoDTO getShowInfo(Long showId, Long sessionId) {
        // 查询演出信息
        Show show = getById(showId);
        if (show == null) {
            throw new BusinessException("演出不存在");
        }
        
        // 查询场次信息
        ShowSession session = showSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("场次不存在");
        }
        
        // 构建返回对象
        ShowInfoDTO showInfoDTO = new ShowInfoDTO();
        showInfoDTO.setShowId(show.getId());
        showInfoDTO.setShowName(show.getName());
        showInfoDTO.setSessionId(session.getId());
        showInfoDTO.setSessionName(session.getName());
        showInfoDTO.setShowTime(session.getStartTime());
        showInfoDTO.setVenue(show.getVenue());
        showInfoDTO.setCity(show.getCity());
        
        return showInfoDTO;
    }
    
    /**
     * 将Show实体转换为ShowVO
     */
    private ShowVO convertToShowVO(Show show) {
        ShowVO showVO = new ShowVO();
        BeanUtils.copyProperties(show, showVO);
        
        // 设置类型名称和状态名称
        showVO.setTypeName(SHOW_TYPE_MAP.getOrDefault(show.getType(), "未知"));
        showVO.setStatusName(SHOW_STATUS_MAP.getOrDefault(show.getStatus(), "未知"));
        
        return showVO;
    }
}