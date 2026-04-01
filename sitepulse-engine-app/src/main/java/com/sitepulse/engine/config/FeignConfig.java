package com.sitepulse.engine.config;

import feign.Logger;
import feign.Request;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(Duration.ofSeconds(10), Duration.ofSeconds(120), true);
    }
}
