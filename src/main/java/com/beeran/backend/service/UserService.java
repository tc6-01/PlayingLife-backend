package com.beeran.backend.service;

import com.beeran.backend.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.beeran.backend.constant.UserConstant.ADMIN_ROLE;
import static com.beeran.backend.constant.UserConstant.USER_LOGIN_STATUS;

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

    /**
     * 根据用户标签搜索用户
     * @param tags 用户使用的标签
     * @return
     */
    List<User> searchUserByTags(List<String> tags);

    Integer updateUser(User user, User loginUser);

    /**
     * 获取当前登录用户信息
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);
    boolean isAdmin(@RequestBody HttpServletRequest request);

    boolean isAdmin(User user);
}
