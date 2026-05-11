package com.sitepulse.engine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SitePulseProperties properties;

    public WebConfig(SitePulseProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(properties.corsOriginArray())
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
