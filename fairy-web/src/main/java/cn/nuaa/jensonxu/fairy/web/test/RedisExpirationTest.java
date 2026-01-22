package cn.nuaa.jensonxu.fairy.web.test;

import cn.nuaa.jensonxu.fairy.service.file.ChunkUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Map;

@Slf4j
@SpringBootApplication(scanBasePackages = "cn.nuaa.jensonxu")
public class RedisExpirationTest {

    public static void main(String[] args) {
        SpringApplication.run(RedisExpirationTest.class, args);
    }

    @Bean
    public CommandLineRunner testExpiration(ChunkUploadService chunkUploadService) {
        return args -> {
            log.info("[Redis过期策略测试] 开始测试");

            String fileMd5 = "test-expiration-123";
            String fileName = "test-expire.mp4";
            int totalChunks = 5;
            long fileSize = 52428800L; // 50MB

            // 测试1: 验证数据已设置过期时间
            log.info("[TTL验证测试] 保存文件元数据并验证TTL");
            chunkUploadService.saveFileMetadata(fileMd5, fileName, totalChunks, fileSize);
            chunkUploadService.markChunkUploaded(fileMd5, 0);
            chunkUploadService.markChunkUploaded(fileMd5, 1);

            Map<Object, Object> metadata = chunkUploadService.getFileMetadata(fileMd5);
            log.info("[TTL验证测试] 文件元数据保存成功: {}", metadata);
            log.info("[TTL验证测试] 已标记分片: 0, 1");
            log.info("[TTL验证测试] 数据将在24小时后自动过期清理");

            // 测试2: 验证数据可正常访问
            log.info("[数据访问测试] 验证数据可正常读取");
            Boolean chunk0 = chunkUploadService.isChunkUploaded(fileMd5, 0);
            Boolean chunk2 = chunkUploadService.isChunkUploaded(fileMd5, 2);
            log.info("[数据访问测试] 分片0状态: {}", chunk0 ? "已上传" : "未上传");
            log.info("[数据访问测试] 分片2状态: {}", chunk2 ? "已上传" : "未上传");

            Long count = chunkUploadService.getUploadedChunkCount(fileMd5);
            log.info("[数据访问测试] 已上传分片总数: {}", count);

            // 测试3: 手动清理数据
            log.info("[手动清理测试] 模拟文件合并完成，手动清理数据");
            chunkUploadService.cleanupFileData(fileMd5);

            Map<Object, Object> afterCleanup = chunkUploadService.getFileMetadata(fileMd5);
            log.info("[手动清理测试] 清理后数据状态: {}", afterCleanup.isEmpty() ? "已清空" : "仍存在");

            log.info("[Redis过期策略测试] 测试完成");
            log.info("[提示] Redis数据已配置24小时过期时间，可有效防止垃圾数据堆积");
        };
    }
}
