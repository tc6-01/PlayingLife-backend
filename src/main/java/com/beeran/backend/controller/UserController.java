package com.beeran.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.beeran.backend.common.BaseResponse;
import com.beeran.backend.common.ErrorCode;
import com.beeran.backend.common.ResultUtils;
import com.beeran.backend.exception.BusisnessException;
import com.beeran.backend.model.domain.User;
import com.beeran.backend.model.domain.request.UserLoginRequest;
import com.beeran.backend.model.domain.request.UserRegisterRequest;
import com.beeran.backend.service.UserService;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.beeran.backend.constant.UserConstant.ADMIN_ROLE;
import static com.beeran.backend.constant.UserConstant.USER_LOGIN_STATUS;


/**
 * 用户接口
 *
 * @Author BeerAn
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;
    // 用户注册
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            return null;
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String password = userRegisterRequest.getPassword();
        String checkPass = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        // controller 提前校验，不涉及业务逻辑
        if (StringUtils.isAnyBlank(userAccount, password, checkPass)) {
            return null;
        }

        long result = userService.userRegister(userAccount, password, checkPass, planetCode);
        return new BaseResponse<>(0,result,"ok","");
    }
    @PostMapping("/logout")
    public  BaseResponse<Integer> userLogout(HttpServletRequest request ){
        if (request == null) {
            throw new BusisnessException(ErrorCode.NULL_ERROR);
        }
        Integer integer = userService.userLogout(request);
        return ResultUtils.Success(integer);
    }

    // 用户登录
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest req) {
        if (userLoginRequest == null) {
            throw new BusisnessException(ErrorCode.NULL_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String password = userLoginRequest.getPassword();
        // controller 提前校验，不涉及业务逻辑
        if (StringUtils.isAnyBlank(userAccount, password)) {
            throw new BusisnessException(ErrorCode.NULL_ERROR);
        }
        User user = userService.userLogin(userAccount, password, req);
        return ResultUtils.Success(user);
    }

    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request){
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATUS);
        User currentUser = (User)userObj;
        if (currentUser == null) {
            throw new BusisnessException(ErrorCode.NO_LOGIN);
        }
        Long id = currentUser.getId();
        // TODO 校验用户是否合法
        User user = userService.getById(id);
        User safetyUser = userService.safetyUser(user);
        return ResultUtils.Success(safetyUser);
    }

    // 查询用户
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String userName, HttpServletRequest request){
        // 鉴权，仅管理员可搜索
        if (!isAdmin(request)){
            throw new BusisnessException(ErrorCode.NO_AUTH);
        }
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(userName)){
            userQueryWrapper.like("username",userName);
        }
        List<User> userList = userService.list(userQueryWrapper);
        List<User> collect = userList.stream().map(user -> {
            user.set_password(null);
            return user;
        }).collect(Collectors.toList());
        return ResultUtils.Success(collect);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUsers(@RequestBody long id, HttpServletRequest request) {

        if (id <= 0) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR);
        }
        if (isAdmin(request)) {
            throw new BusisnessException(ErrorCode.NO_AUTH);
        }
        boolean b = userService.removeById(id);
        return ResultUtils.Success(b);
    }


    private boolean isAdmin(@RequestBody HttpServletRequest request){
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATUS);
        User user = (User) userObj;
        if (user == null || user.getRole() != ADMIN_ROLE){
            return  false;
        }
        return true;
    }

}
