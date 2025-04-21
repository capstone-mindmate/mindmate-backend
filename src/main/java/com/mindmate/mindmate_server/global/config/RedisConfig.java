package com.mindmate.mindmate_server.global.config;

import com.mindmate.mindmate_server.chat.util.ChatMessageListener;
import com.mindmate.mindmate_server.global.util.SuspensionExpirationListener;
import com.mindmate.mindmate_server.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableRedisRepositories
public class RedisConfig {
    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    /**
     * Redis 서버와의 연결 관리
     * LettuceConnectionFactory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(redisHost, redisPort);
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisConfig);

        // 연결 팩토리 반환 전에 Redis 설정을 적용한다?
        connectionFactory.afterPropertiesSet();

        // 연결 후 keyspace 알림 설정 적용 -> 키 만료 이벤트 알림 활성화
        StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
        template.execute((RedisCallback<Object>) connection -> {
            connection.setConfig("notify-keyspace-events", "Ex");
            return null;
        }) ;

        return connectionFactory;
    }

    /**
     * String 타입 템플릿 (key - value 모두 문자열)
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setDefaultSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisTemplate<String, Object> objectRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    /**
     * Redis 구독 설정
     * 채팅방별 채널 구독 설정?ㅇ
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ChatMessageListener chatMessageListener,
            SuspensionExpirationListener suspensionExpirationListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(chatMessageListener, new PatternTopic("chat:room:*")); // 채팅방 관련 이벤트 구독
        container.addMessageListener(chatMessageListener, new PatternTopic("user:status:*")); // 사용자 상태 관련 이벤트 구독

        // 키 만료 이벤트 구독 설정 -> 키 만료됨에 따라 자동적으로 unsuspension 용도
        container.addMessageListener(suspensionExpirationListener, new PatternTopic("__keyevent@*__:expired"));;
        return container;
    }

    /**
     * 정지 만료 이벤트 리스너 등록
     */
    @Bean
    public SuspensionExpirationListener suspensionExpirationListener(UserService userService) {
        return new SuspensionExpirationListener(userService);
    }


}
