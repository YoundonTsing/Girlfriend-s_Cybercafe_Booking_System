package com.ticketsystem.order.service;

import com.alibaba.fastjson.JSON;
import com.ticketsystem.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 订单消息生产者服务
 * 负责发送订单相关的异步消息
 */
@Slf4j
@Service
public class OrderMessageProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 发送订单创建消息
     * @param orderId 订单ID
     * @param orderData 订单数据
     */
    public void sendOrderCreateMessage(Long orderId, Map<String, Object> orderData) {
        try {
            String message = JSON.toJSONString(orderData);
            
            // 异步发送消息
            rocketMQTemplate.asyncSend("order-create", message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("订单创建消息发送成功: orderId={}, msgId={}", orderId, sendResult.getMsgId());
                }

                @Override
                public void onException(Throwable e) {
                    log.error("订单创建消息发送失败: orderId={}, error={}", orderId, e.getMessage(), e);
                }
            });
            
        } catch (Exception e) {
            log.error("发送订单创建消息异常: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    /**
     * 发送订单支付消息
     * @param orderId 订单ID
     * @param paymentData 支付数据
     */
    public void sendOrderPaymentMessage(Long orderId, Map<String, Object> paymentData) {
        try {
            String message = JSON.toJSONString(paymentData);
            
            // 异步发送消息
            rocketMQTemplate.asyncSend("order-payment", message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("订单支付消息发送成功: orderId={}, msgId={}", orderId, sendResult.getMsgId());
                }

                @Override
                public void onException(Throwable e) {
                    log.error("订单支付消息发送失败: orderId={}, error={}", orderId, e.getMessage(), e);
                }
            });
            
        } catch (Exception e) {
            log.error("发送订单支付消息异常: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    /**
     * 发送订单取消消息
     * @param orderId 订单ID
     * @param cancelData 取消数据
     */
    public void sendOrderCancelMessage(Long orderId, Map<String, Object> cancelData) {
        try {
            String message = JSON.toJSONString(cancelData);
            
            // 异步发送消息
            rocketMQTemplate.asyncSend("order-cancel", message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("订单取消消息发送成功: orderId={}, msgId={}", orderId, sendResult.getMsgId());
                }

                @Override
                public void onException(Throwable e) {
                    log.error("订单取消消息发送失败: orderId={}, error={}", orderId, e.getMessage(), e);
                }
            });
            
        } catch (Exception e) {
            log.error("发送订单取消消息异常: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    /**
     * 发送订单状态更新消息
     * @param orderId 订单ID
     * @param statusData 状态数据
     */
    public void sendOrderStatusUpdateMessage(Long orderId, Map<String, Object> statusData) {
        try {
            String message = JSON.toJSONString(statusData);
            
            // 异步发送消息
            rocketMQTemplate.asyncSend("order-status-update", message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("订单状态更新消息发送成功: orderId={}, msgId={}", orderId, sendResult.getMsgId());
                }

                @Override
                public void onException(Throwable e) {
                    log.error("订单状态更新消息发送失败: orderId={}, error={}", orderId, e.getMessage(), e);
                }
            });
            
        } catch (Exception e) {
            log.error("发送订单状态更新消息异常: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    /**
     * 发送库存锁定消息
     * @param orderId 订单ID
     * @param stockData 库存数据
     */
    public void sendStockLockMessage(Long orderId, Map<String, Object> stockData) {
        try {
            String message = JSON.toJSONString(stockData);
            
            // 同步发送消息（库存操作需要确保消息发送成功）
            SendResult sendResult = rocketMQTemplate.syncSend("stock-lock", message);
            log.info("库存锁定消息发送成功: orderId={}, msgId={}", orderId, sendResult.getMsgId());
            
        } catch (Exception e) {
            log.error("发送库存锁定消息异常: orderId={}, error={}", orderId, e.getMessage(), e);
            throw new RuntimeException("库存锁定消息发送失败", e);
        }
    }

    /**
     * 发送库存扣减消息
     * @param orderId 订单ID
     * @param stockData 库存数据
     */
    public void sendStockDeductMessage(Long orderId, Map<String, Object> stockData) {
        try {
            String message = JSON.toJSONString(stockData);
            
            // 同步发送消息（库存操作需要确保消息发送成功）
            SendResult sendResult = rocketMQTemplate.syncSend("stock-deduct", message);
            log.info("库存扣减消息发送成功: orderId={}, msgId={}", orderId, sendResult.getMsgId());
            
        } catch (Exception e) {
            log.error("发送库存扣减消息异常: orderId={}, error={}", orderId, e.getMessage(), e);
            throw new RuntimeException("库存扣减消息发送失败", e);
        }
    }

    /**
     * 发送库存回滚消息
     * @param orderId 订单ID
     * @param stockData 库存数据
     */
    public void sendStockRollbackMessage(Long orderId, Map<String, Object> stockData) {
        try {
            String message = JSON.toJSONString(stockData);
            
            // 同步发送消息（库存操作需要确保消息发送成功）
            SendResult sendResult = rocketMQTemplate.syncSend("stock-rollback", message);
            log.info("库存回滚消息发送成功: orderId={}, msgId={}", orderId, sendResult.getMsgId());
            
        } catch (Exception e) {
            log.error("发送库存回滚消息异常: orderId={}, error={}", orderId, e.getMessage(), e);
            throw new RuntimeException("库存回滚消息发送失败", e);
        }
    }

    /**
     * 发送用户通知消息
     * @param userId 用户ID
     * @param notificationData 通知数据
     */
    public void sendUserNotificationMessage(Long userId, Map<String, Object> notificationData) {
        try {
            String message = JSON.toJSONString(notificationData);
            
            // 异步发送消息
            rocketMQTemplate.asyncSend("user-notification", message, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("用户通知消息发送成功: userId={}, msgId={}", userId, sendResult.getMsgId());
                }

                @Override
                public void onException(Throwable e) {
                    log.error("用户通知消息发送失败: userId={}, error={}", userId, e.getMessage(), e);
                }
            });
            
        } catch (Exception e) {
            log.error("发送用户通知消息异常: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
}