package com.contargo.s3sync;

import com.contargo.s3sync.config.S3Properties;
import com.contargo.s3sync.sync.SyncProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({S3Properties.class, SyncProperties.class})
public class S3SyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(S3SyncApplication.class, args);
    }
}

