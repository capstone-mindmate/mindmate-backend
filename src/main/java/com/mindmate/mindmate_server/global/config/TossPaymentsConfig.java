package com.mindmate.mindmate_server.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConfigurationProperties(prefix = "toss.payments")
@Getter
@Setter
public class TossPaymentsConfig {
    private String clientKey;
    private String secretKey;
    private String successCallbackUrl;
    private String failCallbackUrl;
    private String confirmUrl;

//    @Bean
//    public RestTemplate restTemplate() {
//        return new RestTemplate();
//    }
}