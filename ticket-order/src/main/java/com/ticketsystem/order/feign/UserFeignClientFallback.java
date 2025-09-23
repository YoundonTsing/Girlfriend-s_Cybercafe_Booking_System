package com.ticketsystem.order.feign;

import com.ticketsystem.common.result.Result;
import com.ticketsystem.order.feign.dto.UserInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户服务Feign客户端降级处理
 */
@Component
@Slf4j
public class UserFeignClientFallback implements UserFeignClient {

    @Override
    public Result<UserInfoDTO> getUserInfo(Long userId) {
        log.error("获取用户信息失败，进入降级处理，userId: {}", userId);
        return Result.fail("获取用户信息失败，请稍后再试");
    }
}