package net.codejava.controller;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/redis")
public class RedisTestController {
    private final StringRedisTemplate redis;

    public RedisTestController(StringRedisTemplate redis) { this.redis = redis; }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        redis.opsForValue().set("ping", "pong", Duration.ofSeconds(30));
        return ResponseEntity.ok(redis.opsForValue().get("ping"));
    }

    @PostMapping("/set")
    public ResponseEntity<?> set(@RequestBody Map<String, String> body) {
        String key = body.get("key");
        String value = body.get("value");
        String ttlSec = body.getOrDefault("ttl", "60");
        redis.opsForValue().set(key, value, Duration.ofSeconds(Long.parseLong(ttlSec)));
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/get")
    public ResponseEntity<?> get(@RequestParam String key) {
        return ResponseEntity.ok(Map.of("key", key, "value", redis.opsForValue().get(key)));
    }

    @DeleteMapping("/del")
    public ResponseEntity<?> del(@RequestParam String key) {
        Boolean removed = redis.delete(key);
        return ResponseEntity.ok(Map.of("removed", removed != null && removed));
    }
}
