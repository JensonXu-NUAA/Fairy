package cn.nuaa.jensonxu.fairy.web.controller;

import cn.nuaa.jensonxu.fairy.service.file.FileService;
import cn.nuaa.jensonxu.fairy.service.data.response.CustomResponse;

import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * 简单文件上传接口
     * @param file 上传的文件
     * @return 统一响应格式
     */
    @PostMapping("/upload")
    public CustomResponse<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件是否为空
            if (file.isEmpty()) {
                return CustomResponse.error("[uploadFile] 文件不能为空");
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = Objects.requireNonNull(originalFilename).substring(originalFilename.lastIndexOf("."));
            String objectName = UUID.randomUUID() + extension;
            String fileUrl = baseUrl + "/api/file/download/" + fileService.uploadFile(file, objectName);  // 上传文件

            // 构建响应数据
            Map<String, String> data = new HashMap<>();
            data.put("originalFileName", originalFilename);
            data.put("fileName", objectName);
            data.put("fileUrl", fileUrl);
            data.put("fileSize", String.valueOf(file.getSize()));

            log.info("[uploadFile] 文件上传成功: {}", objectName);
            return CustomResponse.success(data);

        } catch (Exception e) {
            log.error("[uploadFile] 文件上传失败", e);
            return CustomResponse.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 文件下载接口
     * @param fileId 文件ID（对应MinIO中的objectName）
     * @return 文件流
     */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable String fileId) {
        try {
            InputStream fileStream = fileService.getFileStream(fileId);  // 获取文件流
            StatObjectResponse metadata = fileService.getFileMetadata(fileId);  // 获取文件元数据

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(metadata.contentType()));
            headers.setContentLength(metadata.size());

            log.info("[downloadFile] 文件下载: {}", fileId);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(fileStream));

        } catch (Exception e) {
            log.error("[downloadFile] 文件下载失败: {}", fileId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 删除文件接口
     * @param fileId 文件 ID
     * @return 统一响应格式
     */
    @DeleteMapping("/delete/{fileId}")
    public CustomResponse<String> deleteFile(@PathVariable String fileId) {
        try {
            // 检查文件是否存在
            if (!fileService.fileExists(fileId)) {
                return CustomResponse.error("[deleteFile] 文件不存在");
            }

            // 删除文件
            boolean success = fileService.deleteFile(fileId);

            if (success) {
                log.info("[deleteFile] 文件删除成功: {}", fileId);
                return CustomResponse.success("文件删除成功");
            } else {
                return CustomResponse.error("文件删除失败");
            }

        } catch (Exception e) {
            log.error("[deleteFile] 文件删除失败: {}", fileId, e);
            return CustomResponse.error("删除失败: " + e.getMessage());
        }
    }
}
