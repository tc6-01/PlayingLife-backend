package com.beeran.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beeran.backend.model.domain.User;
import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class RedissonTest {
    @Resource
    private RedissonClient redissonClient;
    @Test
    public void test(){
        // list 数据存在JVM内存中
        List<String> list = new ArrayList<>();
        list.add("dsds");
        list.get(0);
        list.remove(0);
        // rlist 存在Redis中
        RList<String> rlist = redissonClient.getList("test-list");
//        rlist.add("BeerAn");
//        rlist.get(0);
        rlist.remove(0);
        // map
        // set
        // stack
    }
    @Test
    void watchDog(){
        RLock lock = redissonClient.getLock("com.user.recommend.lock");
        try {
            // 保证一个线程获取资源
            if (lock.tryLock(0,-1, TimeUnit.MICROSECONDS)) {
                Thread.sleep(10000000);
                System.out.println("get lock" + Thread.currentThread().getName());
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }finally {
            // 判断当前锁是否为自己的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                System.out.println("unlock" + Thread.currentThread().getName());
            }
        }

    }

}
