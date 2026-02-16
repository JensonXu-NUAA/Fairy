package cn.nuaa.jensonxu.fairy.common.file;

import lombok.Builder;
import lombok.Data;

/**
 * 文件处理统一结果
 */
@Data
@Builder
public class FileProcessResult {

    /**
     * 处理是否成功
     */
    private boolean success;

    /**
     * 文件类型
     */
    private FileType fileType;

    /**
     * 文档提取的文本内容（仅 DOCUMENT 类型）
     */
    private String textContent;

    /**
     * 图片 Base64 编码（仅 IMAGE 类型）
     */
    private String base64Content;

    /**
     * MIME 类型（仅 IMAGE 类型，如 image/png）
     */
    private String mimeType;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 错误信息（处理失败时）
     */
    private String errorMessage;

    /**
     * 处理耗时（毫秒）
     */
    private long parseDuration;

    public static FileProcessResult failure(String fileName, String errorMessage) {
        return FileProcessResult.builder()
                .success(false)
                .fileType(FileType.UNSUPPORTED)
                .fileName(fileName)
                .errorMessage(errorMessage)
                .build();
    }
}

