package com.example.demo.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * @author QuiFar
 * @version V1.0
 * @Description: 利用Jedis操作redis工具类, 因为StringRedisTemplate不能用new 注入，必须要用Spring
 * bean方式注入，使用有些地方使用起来不方便
 * @date 2018年1月14日 下午2:50:05
 */
public class JedisUtils {
    private static Logger logger = LoggerFactory.getLogger(JedisUtils.class);

    /**
     * 获取连接
     */
    private static synchronized StringRedisTemplate getRedisTemplate() throws RuntimeException {
        Object bean = SpringContextUtils.getBean("stringRedisTemplate");

        if (Objects.isNull(bean)) {
            throw new RuntimeException("获取 stringRedisTemplate bean 出错！");
        }

        return (StringRedisTemplate) bean;
    }

    /**
     * 获取值操作对象
     *
     * @return
     */
    private static ValueOperations getValueOperations() {
        StringRedisTemplate redisTemplate = getRedisTemplate();
        Assert.notNull(redisTemplate, "redisTemplate is must not be null");
        return getRedisTemplate().opsForValue();
    }

    /**
     * 获取缓存
     *
     * @param key 键
     * @return 值
     */
    public static String get(String key) {
        return Objects.toString(getValueOperations().get(key));
    }

    /**
     * 设置缓存
     *
     * @param key          键
     * @param value        值
     * @param cacheSeconds 超时时间，0为不超时
     * @return
     */
    public static void set(String key, String value, int cacheSeconds) {
        getValueOperations().set(key, value, cacheSeconds);
    }

}
