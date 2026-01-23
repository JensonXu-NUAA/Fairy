package cn.nuaa.jensonxu.fairy.service.data.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 分片上传请求
 */
@Data
public class ChunkUploadDTO {

    /**
     * 文件MD5值
     */
    @NotBlank(message = "文件MD5不能为空")
    private String fileMd5;

    /**
     * 分片索引（从0开始）
     */
    @NotNull(message = "分片索引不能为空")
    @Min(value = 0, message = "分片索引不能为负数")
    private Integer chunkIndex;

    /**
     * 分片文件
     */
    @NotNull(message = "分片文件不能为空")
    private MultipartFile file;
}
