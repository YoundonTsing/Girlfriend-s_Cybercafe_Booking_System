package com.ticketsystem.user.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class LoginDTO implements Serializable {

    @NotBlank(message = "用户名或手机号不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
    
    /**
     * 登录类型：username-用户名登录，phone-手机号登录
     */
    private String loginType;
}