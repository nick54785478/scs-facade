package com.example.scsfacade.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 與 Redisson 全域配置 (RedisConfiguration)。
 * <p>
 * 位於配置層 (config/)，負責組裝 Spring Data Redis 連線工廠、序列化器與 RedissonClient。
 * 為系統提供高可用之 StringRedisTemplate 與具備看門狗續約、防誤刪機制的 Redisson 分散式鎖。
 * </p>
 */
@Configuration
public class RedisConfiguration {

    /**
     * 配置專門用於字串鍵值之 StringRedisTemplate。
     *
     * @param connectionFactory Redis 連線工廠
     * @return StringRedisTemplate 實例
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * 配置通用型 RedisTemplate (以 String 為 Key，JSON 序列化為 Value)。
     *
     * @param connectionFactory Redis 連線工廠
     * @return RedisTemplate 實例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置 RedissonClient，專門用於生產級分散式鎖、看門狗自動續約與線程所有權安全校驗。
     *
     * @param host Redis 主機名稱
     * @param port Redis 埠號
     * @return RedissonClient 實例
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port) {
        Config config = new Config();
        String address = "redis://" + host + ":" + port;
        config.useSingleServer()
                .setAddress(address)
                .setConnectionMinimumIdleSize(4)
                .setConnectionPoolSize(16)
                .setConnectTimeout(5000)
                .setTimeout(3000);
        return Redisson.create(config);
    }
}
