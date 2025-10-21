package com.core_sync.hr_service.redis_cache.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public <T> T getValueByKey(String key, Class<T> valueType) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        
        // Integer를 Long으로 변환
        if (valueType == Long.class && value instanceof Integer) {
            return valueType.cast(((Integer) value).longValue());
        }
        
        return valueType.cast(value);
    }
    
    public void setValueByKey(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }
    
    public void deleteByKey(String key) {
        redisTemplate.delete(key);
    }
}
