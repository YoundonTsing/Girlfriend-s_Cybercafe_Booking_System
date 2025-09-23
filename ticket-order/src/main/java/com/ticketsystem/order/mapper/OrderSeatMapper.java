package com.ticketsystem.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketsystem.order.entity.OrderSeat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单座位关联Mapper接口
 */
@Mapper
public interface OrderSeatMapper extends BaseMapper<OrderSeat> {

    /**
     * 根据订单号查询座位
     * @param orderNo 订单号
     * @return 座位列表
     */
    List<OrderSeat> selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 根据场次ID查询座位
     * @param sessionId 场次ID
     * @return 座位列表
     */
    List<OrderSeat> selectBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 根据订单ID查询座位
     * @param orderId 订单ID
     * @return 座位列表
     */
    List<OrderSeat> selectByOrderId(@Param("orderId") Long orderId);
}