package com.cloudbackup.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class CloudBackupApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudBackupApplication.class, args);
    }
}
