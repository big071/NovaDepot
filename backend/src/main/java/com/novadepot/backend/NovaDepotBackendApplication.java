package com.novadepot.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NovaDepotBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(NovaDepotBackendApplication.class, args);
    }
}
