package com.mindmate.mindmate_server.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Value("${image.dir}")
    private String imageDir;

    @Value("${emoticon.dir}")
    private String emoticonDir;


    @Value("${profile.dir}")
    private String profileDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/magazineImages/**")
                .addResourceLocations("file:" + imageDir);

        registry.addResourceHandler("/emoticonImages/**")
                .addResourceLocations("file:" + emoticonDir);

        registry.addResourceHandler("/profileImages/**")
                .addResourceLocations("file:" + profileDir + "/");

    }
}
