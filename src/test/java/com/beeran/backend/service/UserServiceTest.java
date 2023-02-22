package com.beeran.backend.service;

import com.beeran.backend.model.domain.User;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;


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

    /**
     * 插入用户数据
     */
    @Test
    public void doInsertUsers(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        int final_num = 10000;
        List<User> userList = new ArrayList<>();
        // 插入数据
        for (int i = 0; i < final_num; i++) {
            User user = new User();
            user.setUsername("fakeData");
            user.set_account("fakeYangYang");
            user.setGender((byte)0);
            user.set_password("123456");
            user.setPhone("1111");
            user.setEmail("123@qq.com");
            user.set_status(0);
            user.setRole(0);
            user.setPlanetCode("1212");
            user.setTags("['男']");
            user.setProfile("这是测试的插入数据");
            userList.add(user);
        }
        userService.saveBatch(userList, 1000);
        stopWatch.stop();
        System.out.println("The total cost time is : " + stopWatch.getLastTaskTimeMillis());
    }

    /**
     * 并发插入用户
     */
    @Test
    public void doConcurrenInsertUsers(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        List<CompletableFuture<Void>> featureList = new ArrayList<>();
        int j = 0;
        int batchSize = 5000;
        for (int i = 0; i < 20; i++) {
            List<User> userList = new ArrayList<>();
            // 插入数据
            while (true) {
                j ++;
                if (j%10000==0) break;
                User user = new User();
                user.setUsername("fakeData");
                user.set_account("fakeYangYang");
                user.setGender((byte)1);
                user.set_password("123456");
                user.setPhone("1111");
                user.setEmail("123@qq.com");
                user.set_status(0);
                user.setRole(0);
                user.setPlanetCode("1212");
                user.setTags("['女']");
                user.setProfile("这是测试的插入数据");
                userList.add(user);
            }
            // 异步执行 ，线程池
            CompletableFuture<Void> feature = CompletableFuture.runAsync(() -> {
                System.out.println("threadName: " + Thread.currentThread().getName());
                userService.saveBatch(userList, batchSize);
            });
            featureList.add(feature);
        }
        CompletableFuture.allOf(featureList.toArray(new CompletableFuture[]{})).join();


        stopWatch.stop();
        System.out.println("The total cost time is : " + stopWatch.getLastTaskTimeMillis());
    }
}