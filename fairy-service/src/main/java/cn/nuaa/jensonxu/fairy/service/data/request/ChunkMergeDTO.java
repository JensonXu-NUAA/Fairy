package cn.nuaa.jensonxu.fairy.service.data.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 分片合并请求
 */
@Data
public class ChunkMergeDTO {

    /**
     * 文件MD5值（唯一标识）
     */
    @NotBlank(message = "文件MD5不能为空")
    private String fileMd5;
}
