package com.sitepulse.engine;

import com.sitepulse.engine.config.SitePulseProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableFeignClients
@EnableConfigurationProperties(SitePulseProperties.class)
public class SitePulseEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(SitePulseEngineApplication.class, args);
    }
}
