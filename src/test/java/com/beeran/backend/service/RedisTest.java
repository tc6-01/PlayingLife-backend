package com.beeran.backend.service;

import com.beeran.backend.model.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

@SpringBootTest
public class RedisTest {

    @Resource
    private RedisTemplate redisTemplate;
    @Test
    void test(){

        ValueOperations ops = redisTemplate.opsForValue();
        // 增
        ops.set("string","name");
        ops.set("int",1);
        ops.set("double",2.0);
        User user = new User();
        user.setId(0L);
        user.setUsername("beeran");
        user.set_account("beeran");
        user.setAvatar_url("url");
        ops.set("beeran",user);
        // 删

        // 改

        // 查
        Assertions.assertEquals("name",ops.get("string"));
        Assertions.assertEquals(1,ops.get("int"));
        Assertions.assertEquals(2.0,ops.get("double"));
        System.out.println(ops.get("beeran"));
    }
}
