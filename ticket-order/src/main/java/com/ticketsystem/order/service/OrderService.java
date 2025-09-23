package com.ticketsystem.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticketsystem.order.dto.CreateOrderDTO;
import com.ticketsystem.order.vo.OrderVO;

public interface OrderService {

    /**
     * 创建订单
     * @param createOrderDTO 创建订单DTO
     * @return 订单号
     */
    String createOrder(CreateOrderDTO createOrderDTO);

    /**
     * 支付订单
     * @param orderNo 订单号
     * @param payType 支付方式
     * @return 支付结果
     */
    boolean payOrder(String orderNo, Integer payType);

    /**
     * 取消订单
     * @param orderNo 订单号
     * @param userId 用户ID
     * @return 取消结果
     */
    boolean cancelOrder(String orderNo, Long userId);

    /**
     * 取消订单（系统自动取消，如支付超时）
     * @param orderNo 订单号
     * @return 取消结果
     */
    boolean cancelOrder(String orderNo);

    /**
     * 获取订单详情
     * @param orderNo 订单号
     * @param userId 用户ID
     * @return 订单详情
     */
    OrderVO getOrderDetail(String orderNo, Long userId);

    /**
     * 根据订单ID获取订单详情
     * @param id 订单ID
     * @param userId 用户ID
     * @return 订单详情
     */
    OrderVO getOrderDetailById(Long id, Long userId);

    /**
     * 分页查询用户订单列表
     * @param userId 用户ID
     * @param status 订单状态
     * @param page 页码
     * @param size 每页大小
     * @return 订单列表
     */
    Page<OrderVO> pageUserOrders(Long userId, Integer status, Integer page, Integer size);
}