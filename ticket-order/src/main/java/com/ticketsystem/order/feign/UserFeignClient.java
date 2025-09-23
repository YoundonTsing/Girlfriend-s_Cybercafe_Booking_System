package com.ticketsystem.order.feign;

import com.ticketsystem.common.result.Result;
import com.ticketsystem.order.feign.dto.UserInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户服务Feign客户端
 */
@FeignClient(name = "ticket-user", path = "/api/user", fallback = UserFeignClientFallback.class)
public interface UserFeignClient {

    /**
     * 获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/info/{userId}")
    Result<UserInfoDTO> getUserInfo(@PathVariable("userId") Long userId);
}