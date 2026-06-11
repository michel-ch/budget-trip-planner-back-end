package com.planner.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);

        // Print active profile on startup
        Environment env = context.getEnvironment();

        System.out.println("==============================================");
        System.out.println("Trip Budget Planner Application Started!");
        System.out.println("Active Profile(s): " + String.join(", ", env.getActiveProfiles()));
        System.out.println("Server Port: " + env.getProperty("server.port"));
        System.out.println("Database URL: " + env.getProperty("spring.datasource.url"));
        System.out.println("==============================================");
        System.out.println("✓ Application is ready to accept requests!");
        System.out.println("✓ API available at: http://localhost:" + env.getProperty("server.port") + "/api");
        System.out.println("==============================================");
    }
}