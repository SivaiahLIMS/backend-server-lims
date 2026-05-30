package com.sivayahealth.lims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LimsApplication {
    public static void main(String[] args) {
        // Explicitly force Log4j2 so Spring Boot never picks up Logback,
        // even if it is present on the classpath (e.g. via IDE or transitive dep).
        System.setProperty(
            "org.springframework.boot.logging.LoggingSystem",
            "org.springframework.boot.logging.log4j2.Log4J2LoggingSystem"
        );
        SpringApplication.run(LimsApplication.class, args);
    }
}
