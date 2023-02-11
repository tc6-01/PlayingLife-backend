package com.beeran.backend.service;

import com.beeran.backend.model.domain.User;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;


/**
 * 用户服务测试
 */
@SpringBootTest
class UserServiceTest {

    @Resource
    private UserService userService;

    @Test
    public void testAddUser(){
        User user = new User();
        user.setUsername("dogyangyang");
        user.setPhone("1221212121");
        user.set_account("123456");
        user.set_password("123456");
        user.setAvatar_url("https://baomidou.com/img/logo.svg");
        user.setEmail("12121");

        boolean flag = userService.save(user);
        System.out.println(user.getId());
        Assertions.assertEquals(true, flag);
    }

    @Test
    void userRegister() {
        // 空密码测试
        String userAccount = "BeerAn";
        String userPassword = "";
        String checkPassWord = "123456";
        String plantCode = "121212";
        long result = userService.userRegister(userAccount, userPassword, checkPassWord, plantCode);
        Assertions.assertEquals(-1005, result);
        // 账号长度最小为4位测试
        userAccount = "be";
        userPassword = "123456";
        result = userService.userRegister(userAccount, userPassword, checkPassWord, plantCode);
        Assertions.assertEquals(-1004, result);
        // 两次密码输入不一致 test
        userAccount = "BeerAn";
        userPassword = "678911";
        result = userService.userRegister(userAccount, userPassword, checkPassWord, plantCode);
        Assertions.assertEquals(-1003, result);
        // 密码长度最低为6 test
        userPassword = "1234";
        checkPassWord = "1234";
        result = userService.userRegister(userAccount, userPassword, checkPassWord, plantCode);
        Assertions.assertEquals(-1002, result);
        // 账号不能包含特殊字符 test
        userAccount = "yuyu  yu";
        userPassword = "123456";
        checkPassWord = "123456";
        result = userService.userRegister(userAccount, userPassword, checkPassWord, plantCode);
        Assertions.assertEquals(-1001, result);
        // 账号不能重复 test
        userAccount = "BeerAn";
        result = userService.userRegister(userAccount, userPassword, checkPassWord, plantCode);
        Assertions.assertEquals(-1000, result);
        // 插入数据测试
        userAccount = "yexu";
        userService.userRegister(userAccount, userPassword, checkPassWord, plantCode);

    }

    @Test
    void searchUserByTags() {
        List<String> tagNameList = Arrays.asList("java","python");
        List<User> users = userService.searchUserByTags(tagNameList);
        Assert.assertNotNull(users);
    }
}