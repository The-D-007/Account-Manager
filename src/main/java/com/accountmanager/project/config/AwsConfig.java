package com.accountmanager.project.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
public class AwsConfig {
    private String accessKey = System.getenv("AWS_ACCESS_KEY");
    private String secretKey = System.getenv("AWS_SECRET_KEY");
    private String region = System.getenv("AWS_REGION");
    private String bucketName = System.getenv("AWS_BUCKET_NAME");
}
