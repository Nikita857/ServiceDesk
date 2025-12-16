package com.bm.wschat.shared.redis;

import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthCheck {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisHealthCheck(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void checkRedisConnection() {
        try {
            redisTemplate.opsForValue().set("redis_health_check", "ok");
            String value = (String) redisTemplate.opsForValue().get("redis_health_check");
            if ("ok".equals(value)) {
                System.out.println("Redis подключен и работает!");
            } else {
                System.err.println("Redis подключение установлено, но чтение/запись не работает!");
            }
        } catch (Exception e) {
            System.err.println("Ошибка подключения к Redis: " + e.getMessage());
        }
    }
}

