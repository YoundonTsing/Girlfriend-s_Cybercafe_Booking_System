package com.ticketsystem.user.service;

import com.ticketsystem.user.dto.LoginDTO;
import com.ticketsystem.user.dto.RegisterDTO;
import com.ticketsystem.user.dto.UserInfoDTO;
import com.ticketsystem.user.vo.LoginVO;

public interface UserService {

    /**
     * 用户注册
     */
    void register(RegisterDTO registerDTO);

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 获取用户信息
     */
    UserInfoDTO getUserInfo(Long userId);

    /**
     * 更新用户信息
     */
    void updateUserInfo(UserInfoDTO userInfoDTO);

    /**
     * 修改密码
     */
    void updatePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 退出登录
     */
    void logout(String token);

    /**
     * 获取当前登录用户信息
     */
    UserInfoDTO getCurrentUserInfo();
}