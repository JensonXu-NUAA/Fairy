package cn.nuaa.jensonxu.fairy.repository.redis;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置键值对
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置键值对并指定过期时间
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除键
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 判断键是否存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 存储 Hash 字段
     */
    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * 获取 Hash 字段值
     */
    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    /**
     * 批量存储 Hash
     */
    public void hSetAll(String key, java.util.Map<String, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * 获取 Hash 所有字段
     */
    public java.util.Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 删除 Hash 字段
     */
    public Long hDelete(String key, Object... fields) {
        return redisTemplate.opsForHash().delete(key, fields);
    }

    /**
     * 判断 Hash 字段是否存在
     */
    public Boolean hHasKey(String key, String field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    /**
     * 设置位图中某个位置的值
     * @param key Redis键
     * @param offset 位偏移量（分片索引）
     * @param value true表示已上传，false表示未上传
     */
    public Boolean setBit(String key, long offset, boolean value) {
        return redisTemplate.opsForValue().setBit(key, offset, value);
    }

    /**
     * 获取位图中某个位置的值
     * @param key Redis键
     * @param offset 位偏移量（分片索引）
     * @return true表示已上传，false表示未上传
     */
    public Boolean getBit(String key, long offset) {
        return redisTemplate.opsForValue().getBit(key, offset);
    }

    /**
     * 统计位图中值为1的位数量（已上传的分片数）
     */
    public Long bitCount(String key) {
        return redisTemplate.execute(
                (org.springframework.data.redis.core.RedisCallback<Long>) connection ->
                        connection.stringCommands().bitCount(key.getBytes())
        );
    }
}
