package cn.nuaa.jensonxu.fairy.service.file;

import cn.nuaa.jensonxu.fairy.repository.minio.MinioProperties;

import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import io.minio.PutObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.StatObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.BucketExistsArgs;
import io.minio.StatObjectResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * 上传文件到 MinIO
     * @param file 文件
     * @param objectName 对象名称（文件在MinIO中的路径）
     * @return 文件访问 URL
     */
    public String uploadFile(MultipartFile file, String objectName) throws Exception {
        ensureBucketExists();  // 确保 bucket 存在

        // 上传文件
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        log.info("[minio] 文件上传成功, 文件id: {}", objectName);
        return objectName;
    }

    /**
     * 从 MinIO 获取文件流
     * @param objectName 对象名称
     * @return 文件输入流
     */
    public InputStream getFileStream(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(objectName)
                        .build()
        );
    }

    /**
     * 获取文件的元数据（包括文件大小、类型等）
     * @param objectName 对象名称
     * @return 文件元数据
     */
    public StatObjectResponse getFileMetadata(String objectName) throws Exception {
        return minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(objectName)
                        .build()
        );
    }

    /**
     * 删除文件
     * @param objectName 对象名称
     * @return 是否删除成功
     */
    public boolean deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build()
            );
            log.info("文件删除成功: {}", objectName);
            return true;
        } catch (Exception e) {
            log.error("文件删除失败: {}", objectName, e);
            return false;
        }
    }

    /**
     * 检查文件是否存在
     * @param objectName 对象名称
     * @return 是否存在
     */
    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 确保 bucket 存在，不存在则创建
     */
    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .build()
        );

        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .build()
            );
            log.info("[minio] bucket不存在, 创建bucket: {}", minioProperties.getBucketName());
        }
    }
}
