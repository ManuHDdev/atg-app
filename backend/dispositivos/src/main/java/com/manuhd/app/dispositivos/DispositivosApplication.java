package com.manuhd.app.dispositivos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DispositivosApplication {

    public static void main(String[] args) {
        SpringApplication.run(DispositivosApplication.class, args);
    }
}
