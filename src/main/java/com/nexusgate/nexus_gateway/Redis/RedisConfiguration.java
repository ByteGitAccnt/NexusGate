/*
package com.nexusgate.nexus_gateway.Redis;

import com.nexusgate.nexus_gateway.Config.NexusConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfiguration {

    */
/*@Bean
    public ReactiveRedisConnectionFactory redisConnectionFactory(NexusConfig nexusConfig) {
        var redisConfig = nexusConfig.getRedis();

        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(
                redisConfig.getHost(),redisConfig.getPort());
        return new LettuceConnectionFactory(configuration);
    }*//*


    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        StringRedisSerializer serializer = new StringRedisSerializer();

        RedisSerializationContext<String, String> context =
                RedisSerializationContext
                        .<String, String>newSerializationContext(serializer)
                        .value(serializer)
                        .hashKey(serializer)
                        .hashValue(serializer)
                        .build();

        return new ReactiveRedisTemplate<>(
                connectionFactory,
                context
        );
    }
}*/
