package cn.nuaa.jensonxu.fairy.common.parser.image;

import lombok.Builder;
import lombok.Data;

/**
 * 图片解析结果
 */
@Data
@Builder
public class ImageParseResult {

    /**
     * 解析是否成功
     */
    private boolean success;

    /**
     * 图片的 Base64 编码
     */
    private String base64Content;

    /**
     * MIME 类型，如 image/png, image/jpeg
     */
    private String mimeType;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 原始文件大小（字节）
     */
    private long fileSize;

    /**
     * 解析耗时（毫秒）
     */
    private long parseDuration;

    /**
     * 错误信息（解析失败时）
     */
    private String errorMessage;

    public static ImageParseResult success(String base64Content, String mimeType, String fileName, long fileSize, long parseDuration) {
        return ImageParseResult.builder()
                .success(true)
                .base64Content(base64Content)
                .mimeType(mimeType)
                .fileName(fileName)
                .fileSize(fileSize)
                .parseDuration(parseDuration)
                .build();
    }

    public static ImageParseResult failure(String errorMessage) {
        return ImageParseResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}

