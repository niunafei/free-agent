package com.webmodel.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class WebModelAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebModelAgentApplication.class, args);
    }
}