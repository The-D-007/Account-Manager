package com.accountmanager.project.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI myCustomConfig(){
        return new OpenAPI().info(new Info().title("Account Manager APIs")
                .description("by Ishant")
                )
                .servers(Arrays.asList(new Server().url("http://localhost:8080").description("local"),new Server().url(System.getenv("BACKEND_URL")).description("live")));

    }
}
