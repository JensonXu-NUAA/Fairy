package cn.nuaa.jensonxu.fairy.common.parser.document.impl;

import cn.nuaa.jensonxu.fairy.common.parser.document.DocumentParseResult;
import cn.nuaa.jensonxu.fairy.common.parser.document.DocumentParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.PasswordProvider;
import org.apache.tika.sax.BodyContentHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * 通用文档解析器
 * 使用 Apache Tika 实现，支持多种文档格式
 */
@Slf4j
public class TikaDocumentParser implements DocumentParser {

    private final Tika tika = new Tika();

    /**
     * 支持的文件扩展名
     */
    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".txt", ".csv", ".log", ".ini", ".conf",  // 纯文本
            ".html", ".htm", ".xml", ".json", ".md", ".markdown",  // 标记语言
            ".rtf",  // 富文本
            ".ppt", ".pptx", ".odp",  // 演示文稿
            ".odt", ".ods",  // OpenDocument
            // 代码文件
            ".java", ".py", ".js", ".ts", ".jsx", ".tsx",
            ".go", ".c", ".cpp", ".h", ".hpp", ".cs",
            ".rb", ".php", ".swift", ".kt", ".scala",
            ".rs", ".lua", ".pl", ".r", ".m",
            ".sql", ".sh", ".bash", ".zsh", ".bat", ".ps1",
            ".yaml", ".yml", ".toml", ".properties",
            ".css", ".scss", ".sass", ".less",
            ".vue", ".svelte",
            ".epub",  // 电子书
            ".eml"  // 邮件
    ));

    /**
     * 不支持的二进制文件类型（MIME类型前缀）
     */
    private static final Set<String> UNSUPPORTED_MIME_PREFIXES = new HashSet<>(Arrays.asList(
            "image/",
            "audio/",
            "video/",
            "application/octet-stream",
            "application/x-executable",
            "application/x-msdownload",
            "application/zip",
            "application/x-rar",
            "application/x-7z",
            "application/gzip"
    ));

    @Override
    public DocumentParseResult parse(File file) {
        return parseDocument(file, null, null, null);
    }

    @Override
    public DocumentParseResult parse(InputStream inputStream, String fileName) {
        return parseDocument(null, inputStream, fileName, null);
    }

    @Override
    public DocumentParseResult parseWithPassword(File file, String password) {
        return parseDocument(file, null, null, password);
    }

    @Override
    public DocumentParseResult parseWithPassword(InputStream inputStream, String fileName, String password) {
        return parseDocument(null, inputStream, fileName, password);
    }

    /**
     * 统一的解析逻辑
     */
    private DocumentParseResult parseDocument(File file, InputStream inputStream, String fileName, String password) {
        long startTime = System.currentTimeMillis();

        try {
            String actualFileName;
            if (file != null) {
                actualFileName = file.getName();
            } else {
                actualFileName = fileName;
            }

            String detectedType = tika.detect(actualFileName);  // 检测文件类型
            // 检查是否为不支持的二进制类型
            if (isUnsupportedBinaryType(detectedType)) {
                return DocumentParseResult.failure("不支持的二进制文件类型: " + detectedType);
            }

            InputStream parseStream;
            if (file != null) {
                parseStream = new FileInputStream(file);
            } else {
                parseStream = inputStream;
            }

            // 配置解析器
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1); // -1 表示不限制内容长度
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, actualFileName);  // 设置文件名用于类型检测

            // 如果有密码，配置密码提供者
            if (password != null && !password.isEmpty()) {
                context.set(PasswordProvider.class, metadata1 -> password);
            }

            // 执行解析
            try (parseStream) {
                parser.parse(parseStream, handler, metadata, context);
            }

            // 获取解析结果
            String content = handler.toString().trim();
            String contentType = metadata.get(Metadata.CONTENT_TYPE);
            Map<String, String> metadataMap = extractMetadata(metadata);

            long duration = System.currentTimeMillis() - startTime;

            return DocumentParseResult.builder()
                    .success(true)
                    .content(content)
                    .contentType(contentType != null ? contentType : detectedType)
                    .metadata(metadataMap)
                    .pageCount(parsePageCount(metadata))
                    .charCount(content.length())
                    .encrypted(false)
                    .parseDuration(duration)
                    .build();

        } catch (org.apache.tika.exception.EncryptedDocumentException e) {
            log.warn("[tika] 文档已加密");
            return DocumentParseResult.passwordRequired();
        } catch (Exception e) {
            log.error("[tika] 文档解析失败", e);
            return DocumentParseResult.failure("解析失败: " + e.getMessage());
        }
    }

    /**
     * 检查是否为不支持的二进制类型
     */
    private boolean isUnsupportedBinaryType(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        for (String prefix : UNSUPPORTED_MIME_PREFIXES) {
            if (mimeType.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 提取元数据
     */
    private Map<String, String> extractMetadata(Metadata metadata) {
        Map<String, String> result = new HashMap<>();

        // 标准属性
        putIfNotNull(result, "title", metadata.get(TikaCoreProperties.TITLE));
        putIfNotNull(result, "creator", metadata.get(TikaCoreProperties.CREATOR));
        putIfNotNull(result, "author", metadata.get("Author"));
        putIfNotNull(result, "subject", metadata.get(TikaCoreProperties.SUBJECT));
        putIfNotNull(result, "description", metadata.get(TikaCoreProperties.DESCRIPTION));
        putIfNotNull(result, "keywords", metadata.get("keywords"));
        putIfNotNull(result, "contentType", metadata.get(Metadata.CONTENT_TYPE));

        // 日期属性
        if (metadata.get(TikaCoreProperties.CREATED) != null) {
            result.put("created", metadata.get(TikaCoreProperties.CREATED));
        }
        if (metadata.get(TikaCoreProperties.MODIFIED) != null) {
            result.put("modified", metadata.get(TikaCoreProperties.MODIFIED));
        }

        // 页数（如果有）
        String pageCount = metadata.get("xmpTPg:NPages");
        if (pageCount == null) {
            pageCount = metadata.get("meta:page-count");
        }
        if (pageCount != null) {
            result.put("pageCount", pageCount);
        }

        return result;
    }

    /**
     * 解析页数
     */
    private int parsePageCount(Metadata metadata) {
        String pageCount = metadata.get("xmpTPg:NPages");
        if (pageCount == null) {
            pageCount = metadata.get("meta:page-count");
        }
        if (pageCount != null) {
            try {
                return Integer.parseInt(pageCount);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return 1;
    }

    private void putIfNotNull(Map<String, String> map, String key, String value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
    }

    @Override
    public boolean supports(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lowerName = fileName.toLowerCase();

        // 排除已有专用解析器的格式
        if (lowerName.endsWith(".pdf") ||
                lowerName.endsWith(".doc") || lowerName.endsWith(".docx") ||
                lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx")) {
            return false;
        }

        // 检查是否在支持列表中
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS.toArray(new String[0]);
    }
}
