package com.ticketsystem.user.controller;


import com.ticketsystem.common.exception.BusinessException;
import com.ticketsystem.common.result.Result;
import com.ticketsystem.user.dto.LoginDTO;
import com.ticketsystem.user.dto.RegisterDTO;
import com.ticketsystem.user.dto.UserInfoDTO;
import com.ticketsystem.user.service.UserService;
import com.ticketsystem.user.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO loginDTO) {
        LoginVO loginVO = userService.login(loginDTO);
        return Result.success(loginVO);
    }

    @GetMapping("/info")
    public Result<UserInfoDTO> getUserInfo(HttpServletRequest request) {
        // 从请求头中获取用户ID（由网关设置）
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null) {
            throw new BusinessException("用户未登录");
        }
        Long userId = Long.valueOf(userIdHeader);
        UserInfoDTO userInfoDTO = userService.getUserInfo(userId);
        return Result.success(userInfoDTO);
    }

    @PutMapping("/info")
    public Result<Void> updateUserInfo(@RequestBody UserInfoDTO userInfoDTO) {
        userService.updateUserInfo(userInfoDTO);
        return Result.success();
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@RequestParam Long userId,
                                      @RequestParam String oldPassword,
                                      @RequestParam String newPassword) {
        userService.updatePassword(userId, oldPassword, newPassword);
        return Result.success();
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            userService.logout(token);
        }
        return Result.success();
    }
}