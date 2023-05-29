package com.beeran.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beeran.backend.common.BaseResponse;
import com.beeran.backend.common.ErrorCode;
import com.beeran.backend.common.ResultUtils;
import com.beeran.backend.exception.BusisnessException;
import com.beeran.backend.model.domain.User;
import com.beeran.backend.model.request.UserLoginRequest;
import com.beeran.backend.model.request.UserRegisterRequest;
import com.beeran.backend.model.vo.UserVO;
import com.beeran.backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.beeran.backend.constant.UserConstant.USER_LOGIN_STATUS;


/**
 * 用户接口
 *
 * @Author BeerAn
 */
@RestController
@RequestMapping("/user")
@CrossOrigin
@Slf4j
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
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
        System.out.println("-----------------------------Successfully login-----------------------------");
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
        User user = userService.getById(id);
        User safetyUser = userService.safetyUser(user);
        return ResultUtils.Success(safetyUser);
    }

    // 查询用户
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String userName, HttpServletRequest request){
        // 鉴权，仅管理员可搜索
        if (!userService.isAdmin(request)){
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
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUserByTags(@RequestParam(required = false) List<String> tagNameList){
        // 判断传入数据，请求参数是否为空
        if (CollectionUtils.isEmpty(tagNameList)){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR);
        }
        // 调用Service层
        List<User> userList = userService.searchUserByTags(tagNameList);
        return ResultUtils.Success(userList);
    }
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(long pageSize, long pageNum, HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        Long id = loginUser.getId();
        // 判断当前用户是否有缓存，如果有就直接用缓存
        String redisKey = String.format("com.user.recommend.%s",id);
        ValueOperations<String, Object> sop = redisTemplate.opsForValue();

        Page<User> userPage = (Page<User>) sop.get(redisKey);
        if (userPage != null){
            return ResultUtils.Success(userPage);
        }
        // 无缓存，去查数据库
        // 判断传入数据，请求参数是否为空
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 调用Service层
        userPage = userService.page(new Page<>(pageNum, pageSize),queryWrapper);
        try {
            sop.set(redisKey, userPage, 12, TimeUnit.HOURS);

        } catch (Exception e) {
            log.error("redis key error", e);
        }
        return ResultUtils.Success(userPage);
    }
    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody User user, HttpServletRequest request) {
        /**
         * 1.校验参数是否为空
         * 2.校验权限
         * 3.触发更新
         * todo 如果用户未传递任何信息，则返回空，并抛出异常
         */
        if (user == null){
            throw new BusisnessException(ErrorCode.NULL_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        // 更新用户的属性同时，需要更新登录态
        Integer result = userService.updateUser(user, loginUser, request);
        // 先更新数据库，然后再删除缓存。用户更新的同时需要删除缓存，采用Cache Aside的缓存更新策略
        String redisKey = String.format("com.user.match.%s",loginUser.getId());
        // 写入缓存
        try {
            Boolean flag = redisTemplate.delete(redisKey);
            if (flag == false) {
                throw new BusisnessException(ErrorCode.SYSTEM_ERROR,"更新失败");
            }
        } catch (Exception e) {
            log.error("redis key error", e);
        }
        return ResultUtils.Success(result);

    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUsers(@RequestBody long id, HttpServletRequest request) {

        if (id <= 0) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR);
        }
        if (userService.isAdmin(request)) {
            throw new BusisnessException(ErrorCode.NO_AUTH);
        }
        boolean b = userService.removeById(id);
        return ResultUtils.Success(b);
    }

    /**
     * 根据用户标签进行匹配最合适用户
     *
     * @param num
     * @param request
     * @return
     */
    @GetMapping("/match")
    public BaseResponse<List<UserVO>> matchUser(long num, HttpServletRequest request) {
        if (num < 0 || num >20){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"请求数量不正确");
        }
        User loginUser = userService.getLoginUser(request);
        return  ResultUtils.Success(userService.matchUser(num, loginUser));
    }
    @PostMapping("/upload/avatar")
    public BaseResponse<String> uploadAvatar(MultipartFile avatarImg , HttpServletRequest httpServletRequest){
        if (avatarImg == null) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR);
        }
        String result = userService.uploadAvatar(avatarImg,httpServletRequest);
        return ResultUtils.Success(result);
    }
}
