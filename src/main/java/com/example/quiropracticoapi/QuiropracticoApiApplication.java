package com.example.quiropracticoapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class QuiropracticoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuiropracticoApiApplication.class, args);
    }

}
