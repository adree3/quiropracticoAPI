package com.example.quiropracticoapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootApplication
@EnableAsync
public class QuiropracticoApiApplication {

    public static void main(String[] args) {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
        SpringApplication.run(QuiropracticoApiApplication.class, args);
    }

}
