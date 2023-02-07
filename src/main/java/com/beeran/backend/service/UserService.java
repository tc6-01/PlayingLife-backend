package com.beeran.backend.service;

import com.beeran.backend.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;

/**
 * userService
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册
     * @param userAccount 用户账号
     * @param userPassword 密码
     * @param checkPass 校验密码
     * @return 新用户ID
     */
    long userRegister(String userAccount, String userPassword, String checkPass, String plantCode);

    /**
     * 用户登录
     * @param userAccount 用户账号
     * @param userPassword 密码
     * @return 查询用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest req);

    User safetyUser(User originUser);

    /**
     * 移除用户登录态，退出登录
     * @param request
     * @return
     */
    Integer userLogout(HttpServletRequest request);
}
