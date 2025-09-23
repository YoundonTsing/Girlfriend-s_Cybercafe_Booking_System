package com.ticketsystem.user.controller;

import com.ticketsystem.common.result.Result;
import com.ticketsystem.user.dto.UserInfoDTO;
import com.ticketsystem.user.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户信息接口
 */
@Api(tags = "用户信息接口")
@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserInfoController {

    @Autowired
    private UserService userService;

    /**
     * 获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    @ApiOperation("获取用户信息")
    @GetMapping("/info/{userId}")
    public Result<UserInfoDTO> getUserInfo(
            @ApiParam(value = "用户ID", required = true) @PathVariable Long userId) {
        log.info("获取用户信息，userId: {}", userId);
        UserInfoDTO userInfo = userService.getUserInfo(userId);
        return Result.success(userInfo);
    }

    /**
     * 获取当前登录用户信息
     * @return 用户信息
     */
    @ApiOperation("获取当前登录用户信息")
    @GetMapping("/info/current")
    public Result<UserInfoDTO> getCurrentUserInfo() {
        log.info("获取当前登录用户信息");
        UserInfoDTO userInfo = userService.getCurrentUserInfo();
        return Result.success(userInfo);
    }
}