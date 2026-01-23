package cn.nuaa.jensonxu.fairy.service.data.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 分片状态查询请求
 */
@Data
public class QueryChunkStatusDTO {

    /**
     * 文件MD5值
     */
    @NotBlank(message = "文件MD5不能为空")
    private String fileMd5;
}
