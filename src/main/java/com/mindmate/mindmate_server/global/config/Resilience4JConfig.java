package com.mindmate.mindmate_server.global.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Resilience4JConfig {
    /**
     * 이벤트 발행 장애 상황 담당
     * - 카프카 브로커 연결 실패 or 발행 실패 시 로컬 백업 큐에 저장
     * - 서킷 브레이커로 연속 실패 시 빠른 실패 처리
     * - 브로커 복구 후 백업 큐에서 재발행
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // 50% 실패율에서 서킷 오픈
                .waitDurationInOpenState(Duration.ofMillis(10000)) // 10초 대기 후 half-open
                .permittedNumberOfCallsInHalfOpenState(5) // half-open 상태에서 5번 시도
                .slidingWindowSize(10) // 10버의 호출 기준 실패율 계싼
                .build();

        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }

    @Bean
    public CircuitBreaker kafkaCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker("kafkaCircuitBreaker");
    }

}
