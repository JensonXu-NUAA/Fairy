package cn.nuaa.jensonxu.fairy.common.document.parser.impl;

import cn.nuaa.jensonxu.fairy.common.document.parser.DocumentParseResult;
import cn.nuaa.jensonxu.fairy.common.document.parser.DocumentParser;

import lombok.extern.slf4j.Slf4j;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Word 文档解析器
 * 使用 Apache POI 实现，支持 .doc 和 .docx 格式
 */
@Slf4j
public class WordDocumentParser implements DocumentParser {

    private static final String CONTENT_TYPE_DOC = "application/msword";
    private static final String CONTENT_TYPE_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String[] SUPPORTED_EXTENSIONS = {".doc", ".docx"};

    @Override
    public DocumentParseResult parse(File file) {
        return parseDocument(file, null, null);
    }

    @Override
    public DocumentParseResult parse(InputStream inputStream, String fileName) {
        return parseDocument(null, inputStream, fileName);
    }

    @Override
    public DocumentParseResult parseWithPassword(File file, String password) {
        return parseDocument(file, null, password);
    }

    @Override
    public DocumentParseResult parseWithPassword(InputStream inputStream, String fileName, String password) {
        // POI 对加密 Word 文档的支持较复杂，暂返回提示
        return DocumentParseResult.failure("暂不支持加密 Word 文档的流式解析，请使用文件方式");
    }

    /**
     * 统一的解析逻辑
     */
    private DocumentParseResult parseDocument(File file, InputStream inputStream, String fileNameOrPassword) {
        long startTime = System.currentTimeMillis();

        try {
            String fileName;
            InputStream is;

            if (file != null) {
                fileName = file.getName();
                is = new FileInputStream(file);
            } else {
                fileName = fileNameOrPassword;
                is = inputStream;
            }

            // 根据文件扩展名判断格式
            DocumentParseResult result;
            boolean isDocx = fileName.toLowerCase().endsWith(".docx");
            if (isDocx) {
                result = parseDocx(is);
            } else {
                result = parseDoc(is);
            }

            // 设置解析耗时
            long duration = System.currentTimeMillis() - startTime;
            return DocumentParseResult.builder()
                    .success(result.isSuccess())
                    .content(result.getContent())
                    .contentType(isDocx ? CONTENT_TYPE_DOCX : CONTENT_TYPE_DOC)
                    .metadata(result.getMetadata())
                    .pageCount(result.getPageCount())
                    .charCount(result.getContent() != null ? result.getContent().length() : 0)
                    .encrypted(result.isEncrypted())
                    .errorMessage(result.getErrorMessage())
                    .parseDuration(duration)
                    .build();

        } catch (Exception e) {
            log.error("Word 文档解析失败", e);
            return DocumentParseResult.failure("Word 解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析 .docx 格式 (Office 2007+)
     */
    private DocumentParseResult parseDocx(InputStream inputStream) {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String content = extractor.getText();  // 提取文本
            Map<String, String> metadata = extractDocxMetadata(document);  // 提取元数据
            int paragraphCount = document.getParagraphs().size();  // 获取段落数作为"页数"的近似值

            return DocumentParseResult.builder()
                    .success(true)
                    .content(content)
                    .metadata(metadata)
                    .pageCount(paragraphCount)
                    .build();

        } catch (org.apache.poi.EncryptedDocumentException e) {
            log.warn("[word] Word 文档已加密");
            return DocumentParseResult.passwordRequired();
        } catch (Exception e) {
            log.error("[word] DOCX 解析失败", e);
            return DocumentParseResult.failure("DOCX 解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析 .doc 格式 (Office 97-2003)
     */
    private DocumentParseResult parseDoc(InputStream inputStream) {
        try (BufferedInputStream bis = new BufferedInputStream(inputStream);
             HWPFDocument document = new HWPFDocument(bis);
             WordExtractor extractor = new WordExtractor(document)) {

            String content = extractor.getText();  // 提取文本
            Map<String, String> metadata = extractDocMetadata(document);  // 提取元数据

            return DocumentParseResult.builder()
                    .success(true)
                    .content(content)
                    .metadata(metadata)
                    .pageCount(1) // .doc 格式难以获取准确页数
                    .build();

        } catch (org.apache.poi.EncryptedDocumentException e) {
            log.warn("[word] Word 文档已加密");
            return DocumentParseResult.passwordRequired();
        } catch (Exception e) {
            log.error("[word] DOC 解析失败", e);
            return DocumentParseResult.failure("DOC 解析失败: " + e.getMessage());
        }
    }

    /**
     * 提取 .docx 元数据
     */
    private Map<String, String> extractDocxMetadata(XWPFDocument document) {
        Map<String, String> metadata = new HashMap<>();

        try {
            POIXMLProperties props = document.getProperties();
            POIXMLProperties.CoreProperties coreProps = props.getCoreProperties();

            putIfNotNull(metadata, "title", coreProps.getTitle());
            putIfNotNull(metadata, "creator", coreProps.getCreator());
            putIfNotNull(metadata, "subject", coreProps.getSubject());
            putIfNotNull(metadata, "description", coreProps.getDescription());
            putIfNotNull(metadata, "keywords", coreProps.getKeywords());
            putIfNotNull(metadata, "category", coreProps.getCategory());
            putIfNotNull(metadata, "lastModifiedBy", coreProps.getLastModifiedByUser());

            if (coreProps.getCreated() != null) {
                metadata.put("created", coreProps.getCreated().toString());
            }
            if (coreProps.getModified() != null) {
                metadata.put("modified", coreProps.getModified().toString());
            }
        } catch (Exception e) {
            log.warn("[word] 提取 DOCX 元数据失败", e);
        }

        return metadata;
    }

    /**
     * 提取 .doc 元数据
     */
    private Map<String, String> extractDocMetadata(HWPFDocument document) {
        Map<String, String> metadata = new HashMap<>();

        try {
            var summaryInfo = document.getSummaryInformation();
            if (summaryInfo != null) {
                putIfNotNull(metadata, "title", summaryInfo.getTitle());
                putIfNotNull(metadata, "author", summaryInfo.getAuthor());
                putIfNotNull(metadata, "subject", summaryInfo.getSubject());
                putIfNotNull(metadata, "keywords", summaryInfo.getKeywords());
                putIfNotNull(metadata, "comments", summaryInfo.getComments());
                putIfNotNull(metadata, "lastAuthor", summaryInfo.getLastAuthor());

                if (summaryInfo.getCreateDateTime() != null) {
                    metadata.put("createDate", summaryInfo.getCreateDateTime().toString());
                }
                if (summaryInfo.getLastSaveDateTime() != null) {
                    metadata.put("lastSaveDate", summaryInfo.getLastSaveDateTime().toString());
                }
            }
        } catch (Exception e) {
            log.warn("提取 DOC 元数据失败", e);
        }

        return metadata;
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
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (lowerName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }
}
