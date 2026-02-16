package cn.nuaa.jensonxu.fairy.common.parser.document;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 文档解析结果
 */
@Data
@Builder
public class DocumentParseResult {

    /**
     * 解析是否成功
     */
    private boolean success;

    /**
     * 提取的文本内容
     */
    private String content;

    /**
     * 文档类型
     */
    private String contentType;

    /**
     * 文档元数据（标题、作者、创建时间等）
     */
    private Map<String, String> metadata;

    /**
     * 文档页数（PDF）或工作表数（Excel）
     */
    private int pageCount;

    /**
     * 字符总数
     */
    private long charCount;

    /**
     * 是否为加密文档
     */
    private boolean encrypted;

    /**
     * 错误信息（解析失败时）
     */
    private String errorMessage;

    /**
     * 解析耗时（毫秒）
     */
    private long parseDuration;

    /**
     * 创建成功结果
     */
    public static DocumentParseResult success(String content, String contentType) {
        return DocumentParseResult.builder()
                .success(true)
                .content(content)
                .contentType(contentType)
                .charCount(content != null ? content.length() : 0)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static DocumentParseResult failure(String errorMessage) {
        return DocumentParseResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 创建需要密码的结果
     */
    public static DocumentParseResult passwordRequired() {
        return DocumentParseResult.builder()
                .success(false)
                .encrypted(true)
                .errorMessage("文档已加密，需要提供密码")
                .build();
    }
}
