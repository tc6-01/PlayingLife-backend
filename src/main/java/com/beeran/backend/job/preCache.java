package com.beeran.backend.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beeran.backend.model.domain.User;
import com.beeran.backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存预热
 */
@Component
@Slf4j
public class preCache {
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 判断是否执行完了定时任务
    private boolean flag = false;

    // 重点用户
    private List<Long> mainUsers = Arrays.asList(4L);

    @Scheduled(cron="0 0 7 * * *")
    public void resetExecute(){
        flag = false;
    }
    @Scheduled(cron="0 0 8 * * *")
    public void doCacheRecommendUser(){
        RLock lock = redissonClient.getLock("com.user.recommend.lock");
        try {
            // 保证一个线程获取资源
            if (lock.tryLock(0,10000, TimeUnit.MICROSECONDS) && !flag) {
                System.out.println("get lock" + Thread.currentThread().getName());
                for (Long userId : mainUsers){
                    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                    // 调用Service层
                    Page<User> userPage = userService.page(new Page<>(1, 20),queryWrapper);
                    String redisKey = String.format("com.user.recommend.%s", userId);
                    ValueOperations<String, Object> sop = redisTemplate.opsForValue();

                    // 写入缓存
                    try {
                        sop.set(redisKey, userPage, 12, TimeUnit.HOURS);
                    } catch (Exception e) {
                        log.error("redis key error", e);
                    }
                    flag = true;
                }
            }
        } catch (InterruptedException e) {
            log.error("add lock error",e);
        }finally {
            // 判断当前锁是否为自己的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                System.out.println("unlock" + Thread.currentThread().getName());
            }
        }

    }
}
