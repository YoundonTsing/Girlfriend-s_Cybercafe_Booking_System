package com.ticketsystem.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ticketsystem.common.exception.BusinessException;
import com.ticketsystem.common.result.ResultCode;
import com.ticketsystem.user.dto.LoginDTO;
import com.ticketsystem.user.dto.RegisterDTO;
import com.ticketsystem.user.dto.UserInfoDTO;
import com.ticketsystem.user.entity.User;
import com.ticketsystem.user.mapper.UserMapper;
import com.ticketsystem.user.service.UserService;
import com.ticketsystem.user.util.JwtUtil;
import com.ticketsystem.user.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
// import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    // private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtil jwtUtil;

    private static final String TOKEN_PREFIX = "user:token:";
    private static final long TOKEN_EXPIRE_TIME = 24; // 24小时

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO registerDTO) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(User::getUsername, registerDTO.getUsername());
        if (baseMapper.selectCount(usernameWrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 检查手机号是否已存在
        LambdaQueryWrapper<User> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(User::getPhone, registerDTO.getPhone());
        if (baseMapper.selectCount(phoneWrapper) > 0) {
            throw new BusinessException("手机号已被注册");
        }

        // 检查邮箱是否已存在
        if (registerDTO.getEmail() != null && !registerDTO.getEmail().isEmpty()) {
            LambdaQueryWrapper<User> emailWrapper = new LambdaQueryWrapper<>();
            emailWrapper.eq(User::getEmail, registerDTO.getEmail());
            if (baseMapper.selectCount(emailWrapper) > 0) {
                throw new BusinessException("邮箱已被注册");
            }
        }

        // 创建用户
        User user = new User();
        BeanUtil.copyProperties(registerDTO, user);
        // 设置默认昵称
        if (user.getNickname() == null || user.getNickname().isEmpty()) {
            user.setNickname(user.getUsername());
        }
        // 加密密码
        user.setPassword(BCrypt.hashpw(registerDTO.getPassword()));
        // 设置状态为正常
        user.setStatus(1);
        // 设置创建/更新时间，避免插入 null 触发 DB 约束
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        // 保存用户
        baseMapper.insert(user);
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        log.info("[login] username={}, loginType={}, traceId={}", loginDTO.getUsername(), loginDTO.getLoginType(), IdUtil.getSnowflakeNextId());
        
        // 查询用户 - 支持用户名和手机号登录
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        // 根据登录类型构建查询条件
        if ("phone".equals(loginDTO.getLoginType())) {
            // 手机号登录
            wrapper.eq(User::getPhone, loginDTO.getUsername());
        } else {
            // 用户名登录（默认）
            wrapper.eq(User::getUsername, loginDTO.getUsername());
        }
        
        User user = baseMapper.selectOne(wrapper);
        if (user == null) {
            String errorMessage = "phone".equals(loginDTO.getLoginType()) ? "手机号或密码错误" : "用户名或密码错误";
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), errorMessage);
        }

        // 检查用户状态
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        // 验证密码：优先BCrypt，失败则尝试与常见散列兼容（如初始化脚本中的MD5）
        boolean passwordMatches = false;
        try {
            passwordMatches = BCrypt.checkpw(loginDTO.getPassword(), user.getPassword());
        } catch (Exception ignore) {
            // 如果存储的不是BCrypt格式，继续尝试MD5兼容
        }

        if (!passwordMatches) {
            // 兼容：若库里是MD5(123456)这类旧数据
            String md5OfInput = cn.hutool.crypto.digest.DigestUtil.md5Hex(loginDTO.getPassword());
            if (md5OfInput.equalsIgnoreCase(user.getPassword())) {
                passwordMatches = true;
            }
        }

        if (!passwordMatches) {
            String errorMessage = "phone".equals(loginDTO.getLoginType()) ? "手机号或密码错误" : "用户名或密码错误";
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), errorMessage);
        }

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        baseMapper.updateById(user);
        log.info("[login] success userId={}", user.getId());

        // 生成token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 将token存入Redis - 暂时禁用
        // redisTemplate.opsForValue().set(TOKEN_PREFIX + token, user.getId(), TOKEN_EXPIRE_TIME, TimeUnit.HOURS);

        // 返回登录信息
        return LoginVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .token(token)
                .build();
    }

    @Override
    public UserInfoDTO getUserInfo(Long userId) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        UserInfoDTO userInfoDTO = new UserInfoDTO();
        BeanUtil.copyProperties(user, userInfoDTO);
        return userInfoDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(UserInfoDTO userInfoDTO) {
        User user = baseMapper.selectById(userInfoDTO.getId());
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }

        // 检查手机号是否已被其他用户使用
        if (userInfoDTO.getPhone() != null && !userInfoDTO.getPhone().equals(user.getPhone())) {
            LambdaQueryWrapper<User> phoneWrapper = new LambdaQueryWrapper<>();
            phoneWrapper.eq(User::getPhone, userInfoDTO.getPhone())
                    .ne(User::getId, userInfoDTO.getId());
            if (baseMapper.selectCount(phoneWrapper) > 0) {
                throw new BusinessException("手机号已被其他用户使用");
            }
        }

        // 检查邮箱是否已被其他用户使用
        if (userInfoDTO.getEmail() != null && !userInfoDTO.getEmail().equals(user.getEmail())) {
            LambdaQueryWrapper<User> emailWrapper = new LambdaQueryWrapper<>();
            emailWrapper.eq(User::getEmail, userInfoDTO.getEmail())
                    .ne(User::getId, userInfoDTO.getId());
            if (baseMapper.selectCount(emailWrapper) > 0) {
                throw new BusinessException("邮箱已被其他用户使用");
            }
        }

        // 更新用户信息
        User updateUser = new User();
        BeanUtil.copyProperties(userInfoDTO, updateUser);
        baseMapper.updateById(updateUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }

        // 验证旧密码
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }

        // 更新密码
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPassword(BCrypt.hashpw(newPassword));
        baseMapper.updateById(updateUser);
    }

    @Override
    public void logout(String token) {
        // 从Redis中删除token - 暂时禁用
        // redisTemplate.delete(TOKEN_PREFIX + token);
    }

    @Override
    public UserInfoDTO getCurrentUserInfo() {
        // 从上下文中获取当前用户ID
        Long userId = com.ticketsystem.common.util.UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        return getUserInfo(userId);
    }
}