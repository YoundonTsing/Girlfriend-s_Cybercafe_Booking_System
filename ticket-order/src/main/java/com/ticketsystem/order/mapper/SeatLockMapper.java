package com.ticketsystem.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketsystem.order.entity.SeatLock;
import org.apache.ibatis.annotations.Mapper;

/**
 * 座位锁定Mapper接口
 */
@Mapper
public interface SeatLockMapper extends BaseMapper<SeatLock> {
}