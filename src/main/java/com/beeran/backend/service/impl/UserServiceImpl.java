package com.beeran.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.beeran.backend.common.ErrorCode;
import com.beeran.backend.exception.BusisnessException;
import com.beeran.backend.model.domain.User;
import com.beeran.backend.service.UserService;
import com.beeran.backend.mapper.UserMapper;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.beeran.backend.constant.UserConstant.USER_LOGIN_STATUS;


/**
 * 用户服务实现类
 *
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
}




