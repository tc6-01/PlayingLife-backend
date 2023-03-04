package com.beeran.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.beeran.backend.common.ErrorCode;
import com.beeran.backend.exception.BusisnessException;
import com.beeran.backend.mapper.UserMapper;
import com.beeran.backend.model.domain.User;
import com.beeran.backend.service.UserService;
import com.beeran.backend.utils.AlgorithmUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.beeran.backend.constant.UserConstant.ADMIN_ROLE;
import static com.beeran.backend.constant.UserConstant.USER_LOGIN_STATUS;


/**
 * 用户服务实现类
 * @author BeerAn
 */
@Service
@MapperScan("com.beeran.backend.model.domain.User")
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{
    private static final String SALT = "beeran";

    @Resource
    private UserMapper userMapper;

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPass, String planetCode) {
        //1.校验
        if (StringUtils.isAnyBlank(userAccount,userPassword,checkPass, planetCode)){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "账号，密码,校验密码或星球编号为空");
        }
        if (userAccount.length() < 4){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "账号长度最小为4位");
        }
        if (!userPassword.equals(checkPass)){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "两次密码输入不一致");
        }
        if (userPassword.length() < 6){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "密码长度最低为6");
        }

        // 账号不能包含特殊字符
        String pattern = "\\pP|\\pS|\\s";
        Matcher matcher = Pattern.compile(pattern).matcher(userAccount);
        if (matcher.find()){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "不能使用特殊字符");
        }

        // 账号不能重复
        QueryWrapper<User> queryAccount = new QueryWrapper<>();
        queryAccount.eq("_account", userAccount);
        long countAccount = this.count(queryAccount);
        if (countAccount > 0){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "已存在相同账号");
        }

        // 星球编号不能重复
        QueryWrapper<User> queryPlantCode = new QueryWrapper<>();
        queryPlantCode.eq("planetCode", planetCode);
        long countPlant = this.count(queryPlantCode);
        if (countPlant > 0){
           throw new BusisnessException(ErrorCode.PARAMS_ERROR, "当前星球编号已注册");
        }
        // 2.加密

        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        // 3.插入数据
        User user = new User();
        user.set_account(userAccount);
        user.set_password(encryptPassword);
        user.setPlanetCode(planetCode);
        boolean save = this.save(user);
        if (!save){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "插入数据失败");
        }
        return user.getId();
    }


    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest req) {
        //1.校验
        if (StringUtils.isAnyBlank(userAccount,userPassword)){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR, "账号或密码为空");
        }
        if (userAccount.length() < 4){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"账号长度最小为4位");
        }

        if (userPassword.length() < 6){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"密码长度最低为6");
        }

        // 账号不能包含特殊字符
        String pattern = "\\pP|\\pS|\\s";
        Matcher matcher = Pattern.compile(pattern).matcher(userAccount);
        if (matcher.find()){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"账号中不能使用特殊字符");
        }
        // 2.加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("_account",userAccount);
        queryWrapper.eq("_password",encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null){
            throw new BusisnessException(ErrorCode.PARAMS_ERROR,"账户或密码错误,请重试");
        }
        // 3.用户脱敏
        User safeUser = safetyUser(user);
        // 4.记录用户登录状态
        req.getSession().setAttribute(USER_LOGIN_STATUS, safeUser);
//        System.out.println(req.getSession().getAttribute(USER_LOGIN_STATUS));
//        System.out.println("成功设置用户登录态");
        return safeUser;

    }

    /**
     * 用户脱敏
     * @param originUser 未脱敏用户对象
     * @return
     */
    @Override
    public User safetyUser(User originUser){
        if (originUser == null) {
            return null;
        }
        User safeUser = new User();
        safeUser.setId(originUser.getId());
        safeUser.setUsername(originUser.getUsername());
        safeUser.set_account(originUser.get_account());
        safeUser.setAvatar_url(originUser.getAvatar_url());
        safeUser.setGender(originUser.getGender());
        safeUser.setPhone(originUser.getPhone());
        safeUser.setPlanetCode(originUser.getPlanetCode());
        safeUser.setEmail(originUser.getEmail());
        safeUser.setRole(originUser.getRole());
        safeUser.set_status(originUser.get_status());
        safeUser.setCreate_time(originUser.getCreate_time());
        safeUser.setTags(originUser.getTags());
        safeUser.setProfile(originUser.getProfile());
        return safeUser;
    }

    /**
     * 用户注销，移除登录态
     * @param request
     */
    @Override
    public Integer userLogout(HttpServletRequest request) {
        request.getSession().removeAttribute(USER_LOGIN_STATUS);
        return 0;
    }

    /**
     * 根据标签查询用户 <SQL 查询版>
     * @param tagNameList 用户使用的标签
     * @return
     */
    @Override
    public List<User> searchUserByTags(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR);
        }
        // 拼接 and 查询
        // SQL语句：Like %JAVA% AND LIKE %PYTHON%
        long start = System.currentTimeMillis();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        for (String tagName : tagNameList) {
            queryWrapper = queryWrapper.like("tags", tagName);
        }
        List<User> users = userMapper.selectList(queryWrapper);
        System.out.println("sql cost time:" + (System.currentTimeMillis() - start));
        return users.stream().map(this::safetyUser).collect(Collectors.toList());
    }

    /**
     * 根据标签查询用户 <memory 查询版>
     * @param tagNameList 用户使用的标签
     * @return
     */
    @Deprecated
    private List<User> searchUserByTagsByMemory(List<String> tagNameList){
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusisnessException(ErrorCode.PARAMS_ERROR);
        }
        // 先查询所有用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        long start = System.currentTimeMillis();
        List<User> users = userMapper.selectList(queryWrapper);
        Gson gson = new Gson();
        // 在内存中判断是否有需要的标签
        // 语法糖--->代替遍历内存中的每条记录
        users = users.stream().filter(user -> {
            String tags = user.getTags();
            // 将获取的json字符串反序列化，存储在集合中
            // 遍历传入的列表参数，查找所有标签都存在的记录
            // 如果集合中有对应的标签，就将该用户放入
            if (StringUtils.isBlank(tags)) return false;
            Set<String> tagNameSet = gson.fromJson(tags, new TypeToken<Set<String>>() {}.getType());
            tagNameSet = Optional.ofNullable(tagNameSet).orElse(new HashSet<>());
            for (String tagName : tagNameList) {
                if (!tagNameSet.contains(tagName)) {
                    return false;
                }
            }
            return true;
        }).map(this::safetyUser).collect(Collectors.toList());
        System.out.println("memory cost time:" + (System.currentTimeMillis() - start));
        return users;
    }

    /**
     * 更新用户信息
     * @param user 修改后的用户信息
     * @param loginUser 登录的用户信息
     * @return 更新后的用户ID
     */
    @Override
    public Integer updateUser(User user, User loginUser) {
        Long userID = user.getId();
        if (userID == null){
            throw new BusisnessException(ErrorCode.NULL_ERROR);
        }
        // 只有管理员和自己可以修改
        if (isAdmin(loginUser) && userID != loginUser.getId()) {
            throw new BusisnessException(ErrorCode.NO_AUTH);
        }
        User oldUser = userMapper.selectById(userID);
        if (oldUser == null) {
            throw new BusisnessException(ErrorCode.NULL_ERROR);
        }
        // 仅管理员和用户自己可修改
        return userMapper.updateById(user);
    }

    /**
     * 从cookie中判断用户是否登录
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATUS);
        if (userObj == null) {
            throw new BusisnessException(ErrorCode.NO_LOGIN);
        }
        return (User) userObj;
    }

    /**
     * 判断当前登录用户是否为管理员
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATUS);
        User user = (User) userObj;
        if (user == null || user.getRole() != ADMIN_ROLE){
            return  false;
        }
        return true;
    }

    /**
     * 判断传入用户是否为管理员
     * @param user
     * @return
     */
    @Override
    public boolean isAdmin(User user) {
        if (user == null || user.getRole() != ADMIN_ROLE){
            return  false;
        }
        return true;
    }

    @Override
    public List<User> matchUser(long num, User loginUser) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "tags");
        queryWrapper.isNotNull("tags");
        List<User> userList = this.list(queryWrapper);
        String tags = loginUser.getTags();
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        // 用户列表的下标 => 相似度
        List<Pair<User, Long>> list = new ArrayList<>();
        // 依次计算所有用户和当前用户的相似度
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            String userTags = user.getTags();
            // 无标签或者为当前用户自己
            if (StringUtils.isBlank(userTags) || user.getId() == loginUser.getId()) {
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            // 计算分数
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            list.add(new Pair<>(user, distance));
        }
        // 按编辑距离由小到大排序
        List<Pair<User, Long>> topUserPairList = list.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());
        // 原本顺序的 userId 列表
        List<Long> userIdList = topUserPairList.stream().map(pair -> pair.getKey().getId()).collect(Collectors.toList());
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id", userIdList);
        // 1, 3, 2
        // User1、User2、User3
        // 1 => User1, 2 => User2, 3 => User3
        Map<Long, List<User>> userIdUserListMap = this.list(userQueryWrapper)
                .stream()
                .map(user -> safetyUser(user))
                .collect(Collectors.groupingBy(User::getId));
        List<User> finalUserList = new ArrayList<>();
        for (Long userId : userIdList) {
            finalUserList.add(userIdUserListMap.get(userId).get(0));
        }
        return finalUserList;
    }

}




