package com.jinhan.TrafficBlog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@Profile("batch")
public class BatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrafficBlogApplication.BatchApplication.class, args);
    }

}