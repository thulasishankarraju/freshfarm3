package com.example.freshfarm3;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
@SpringBootApplication
@EnableAsync
public class Freshfarm3Application {
    public static void main(String[] args) {
        SpringApplication.run(Freshfarm3Application.class, args);
    }
}
