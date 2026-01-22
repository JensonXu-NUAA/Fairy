package cn.nuaa.jensonxu.fairy.web.test;

import cn.nuaa.jensonxu.fairy.repository.redis.RedisUtil;
import cn.nuaa.jensonxu.fairy.service.file.ChunkUploadService;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Map;

@Slf4j
@SpringBootApplication(scanBasePackages = "cn.nuaa.jensonxu")
@MapperScan("cn.nuaa.jensonxu.fairy.repository.mysql.mapper")
public class RedisTestMain {

    public static void main(String[] args) {
        SpringApplication.run(RedisTestMain.class, args);
    }

    @Bean
    public CommandLineRunner testRedis(RedisUtil redisUtil, ChunkUploadService chunkUploadService) {
        return args -> {
            log.info("[Redis基础测试] 开始测试Redis连接");

            // 测试1: 基本String操作
            log.info("[String操作测试] 开始测试");
            redisUtil.set("test:key", "Hello Redis");
            Object value = redisUtil.get("test:key");
            log.info("[String操作测试] 存储的值: {}", value);
            log.info("[String操作测试] 键是否存在: {}", redisUtil.hasKey("test:key"));

            // 测试2: Hash操作
            log.info("[Hash操作测试] 开始测试");
            redisUtil.hSet("test:hash", "field1", "value1");
            redisUtil.hSet("test:hash", "field2", "value2");
            Map<Object, Object> hashData = redisUtil.hGetAll("test:hash");
            log.info("[Hash操作测试] Hash数据: {}", hashData);

            // 测试3: BitSet操作
            log.info("[BitSet操作测试] 开始测试");
            redisUtil.setBit("test:bitmap", 0, true);
            redisUtil.setBit("test:bitmap", 2, true);
            redisUtil.setBit("test:bitmap", 5, true);
            log.info("[BitSet操作测试] 第0位: {}", redisUtil.getBit("test:bitmap", 0));
            log.info("[BitSet操作测试] 第1位: {}", redisUtil.getBit("test:bitmap", 1));
            log.info("[BitSet操作测试] 第2位: {}", redisUtil.getBit("test:bitmap", 2));
            log.info("[BitSet操作测试] 值为1的位数: {}", redisUtil.bitCount("test:bitmap"));

            log.info("[Redis基础测试] Redis基本功能测试完成");
        };
    }

    @Bean
    public CommandLineRunner testChunkUpload(ChunkUploadService chunkUploadService) {
        return args -> {
            log.info("[分片上传场景测试] 开始测试分片上传场景");

            // 模拟文件信息
            String fileMd5 = "abc123def456";
            String fileName = "test-video.mp4";
            int totalChunks = 10;
            long fileSize = 104857600L; // 100MB

            // 测试1: 保存文件元数据
            log.info("[元数据保存测试] 保存文件元数据");
            chunkUploadService.saveFileMetadata(fileMd5, fileName, totalChunks, fileSize);
            Map<Object, Object> metadata = chunkUploadService.getFileMetadata(fileMd5);
            log.info("[元数据保存测试] 文件元数据: {}", metadata);

            // 测试2: 模拟分片上传
            log.info("[分片上传测试] 模拟分片上传");
            chunkUploadService.markChunkUploaded(fileMd5, 0);
            chunkUploadService.markChunkUploaded(fileMd5, 2);
            chunkUploadService.markChunkUploaded(fileMd5, 5);
            chunkUploadService.markChunkUploaded(fileMd5, 7);
            log.info("[分片上传测试] 已标记分片: 0, 2, 5, 7");

            // 测试3: 查询分片状态
            log.info("[分片状态查询测试] 查询分片上传状态");
            for (int i = 0; i < totalChunks; i++) {
                Boolean uploaded = chunkUploadService.isChunkUploaded(fileMd5, i);
                log.info("[分片状态查询测试] 分片{}状态: {}", i, (uploaded ? "已上传" : "未上传"));
            }

            // 测试4: 统计上传进度
            log.info("[上传进度统计测试] 统计上传进度");
            Long uploadedCount = chunkUploadService.getUploadedChunkCount(fileMd5);
            log.info("[上传进度统计测试] 已上传分片数: {}/{}", uploadedCount, totalChunks);
            double progress = (uploadedCount * 100.0) / totalChunks;
            log.info("[上传进度统计测试] 上传进度: {}%", String.format("%.2f", progress));

            // 测试5: 断点续传场景
            log.info("[断点续传测试] 模拟断点续传");
            StringBuilder needUpload = new StringBuilder();
            for (int i = 0; i < totalChunks; i++) {
                if (!chunkUploadService.isChunkUploaded(fileMd5, i)) {
                    needUpload.append(i).append(" ");
                }
            }
            log.info("[断点续传测试] 客户端查询需要上传的分片: {}", needUpload.toString().trim());
            log.info("[断点续传测试] 需要续传的分片: 1, 3, 4, 6, 8, 9");

            // 测试6: 模拟合并完成清理
            log.info("[数据清理测试] 模拟文件合并完成后清理");
            chunkUploadService.cleanupFileData(fileMd5);
            Map<Object, Object> afterCleanup = chunkUploadService.getFileMetadata(fileMd5);
            log.info("[数据清理测试] 清理后查询元数据: {}", (afterCleanup.isEmpty() ? "数据已清空" : afterCleanup));

            log.info("[分片上传场景测试] 分片上传场景测试完成");
        };
    }
}