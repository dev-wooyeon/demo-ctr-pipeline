package com.example.ctr;

import com.example.ctr.application.CtrJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class CtrApplication implements CommandLineRunner {

    static {
        // Disable Spring Boot's logging system so that Flink's Log4j stack handles bindings.
        System.setProperty(
                "org.springframework.boot.logging.LoggingSystem",
                System.getProperty("org.springframework.boot.logging.LoggingSystem", "none"));
    }

    private final CtrJobService ctrJobService;

    public static void main(String[] args) {
        SpringApplication.run(CtrApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        ctrJobService.execute();
    }
}
