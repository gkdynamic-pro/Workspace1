package net.codejava.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisStartupTest implements CommandLineRunner {
    private final StringRedisTemplate redis;

    public RedisStartupTest(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void run(String... args) {
        redis.opsForValue().set("hello", "world");
        System.out.println("Redis says: " + redis.opsForValue().get("hello"));
    }
}
