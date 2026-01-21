package cn.nuaa.jensonxu.fairy.repository.minio;

import lombok.Data;

import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    /**
     * MinIO 服务地址
     */
    private String endpoint;

    /**
     * Access Key
     */
    private String accessKey;

    /**
     * Secret Key
     */
    private String secretKey;

    /**
     * 存储桶名称
     */
    private String bucketName;
}
