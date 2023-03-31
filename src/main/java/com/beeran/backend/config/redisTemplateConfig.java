package com.beeran.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class redisTemplateConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
        // 建立模板
        RedisTemplate<String, Object> stringObjectRedisTemplate = new RedisTemplate<>();
        // 建立连接工厂
        stringObjectRedisTemplate.setConnectionFactory(redisConnectionFactory);
        // 设置key的序列化工具
        stringObjectRedisTemplate.setKeySerializer(RedisSerializer.string());
        // 设置值的序列化工具
//        stringObjectRedisTemplate.setValueSerializer(RedisSerializer.string());
        return stringObjectRedisTemplate;
    }
}
