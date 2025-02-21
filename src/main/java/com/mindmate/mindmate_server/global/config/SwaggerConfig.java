package com.mindmate.mindmate_server.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MindMate API Documentation")
                        .version("v1")
                        .description("MindMate API 명세서"))
                .servers(Arrays.asList(
                        new Server().url("http://localhost:8080")
                ));
    }
}
