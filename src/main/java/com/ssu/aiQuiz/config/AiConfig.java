package com.ssu.aiQuiz.config;

import com.zhipu.oapi.ClientV4;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class AiConfig {

    /**
     * ApiKey，智谱Ai
     */
    private String apiKey;

    @Bean
    public ClientV4 getClientV4(){
        return new ClientV4.Builder(apiKey).networkConfig(10000,10000,10000,10000, TimeUnit.SECONDS).build();
    }
}
