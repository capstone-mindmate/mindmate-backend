package com.mindmate.mindmate_server.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@Configuration
public class JpaConfig {
    /**
     * 향후 auditorProvider, dateTimeProvider 등 상세 내용 적용시 코드 추가
     */
}
