package cn.nuaa.jensonxu.fairy.common.document.parser.impl;

import cn.nuaa.jensonxu.fairy.common.document.parser.DocumentParseResult;
import cn.nuaa.jensonxu.fairy.common.document.parser.DocumentParser;

import lombok.extern.slf4j.Slf4j;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * PDF 文档解析器
 * 使用 Apache PDFBox 实现
 */
@Slf4j
public class PdfDocumentParser implements DocumentParser {

    private static final String CONTENT_TYPE = "application/pdf";
    private static final String[] SUPPORTED_EXTENSIONS = {".pdf"};


    @Override
    public DocumentParseResult parse(File file) {
        return parseDocument(file, null, null);
    }

    @Override
    public DocumentParseResult parse(InputStream inputStream, String fileName) {
        return parseDocument(null, inputStream, null);
    }

    @Override
    public DocumentParseResult parseWithPassword(File file, String password) {
        return parseDocument(file, null, password);
    }

    @Override
    public DocumentParseResult parseWithPassword(InputStream inputStream, String fileName, String password) {
        return parseDocument(null, inputStream, password);
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

    /**
     * 统一的解析逻辑
     */
    private DocumentParseResult parseDocument(File file, InputStream inputStream, String password) {
        long startTime = System.currentTimeMillis();
        try {
            PDDocument document;
            if(file != null) {
                document = password != null
                        ? Loader.loadPDF(file, password)
                        : Loader.loadPDF(file);
            } else {
                document = password != null
                        ? Loader.loadPDF(new RandomAccessReadBuffer(inputStream), password)
                        : Loader.loadPDF(new RandomAccessReadBuffer(inputStream));
            }

            try (document) {
                if (document.isEncrypted() && password == null) {
                    return DocumentParseResult.passwordRequired();  // 检查是否加密且未解锁
                }

                PDFTextStripper stripper = new PDFTextStripper();
                String content = stripper.getText(document);  // 提取文本
                Map<String, String> metaData = extractMetadata(document);  // 提取元数据
                int pageCount = document.getNumberOfPages();  // 获取页数

                return DocumentParseResult.builder()
                        .success(true)
                        .content(content)
                        .contentType(CONTENT_TYPE)
                        .metadata(metaData)
                        .pageCount(pageCount)
                        .charCount(content.length())
                        .encrypted(document.isEncrypted())
                        .parseDuration(System.currentTimeMillis() - startTime)
                        .build();
            }
        } catch (InvalidPasswordException e) {
            log.warn("[pdf] PDF 密码错误或文档已加密");
            return DocumentParseResult.passwordRequired();
        } catch (Exception e) {
            log.error("[pdf] PDF 解析失败", e);
            return DocumentParseResult.failure("PDF 解析失败: " + e.getMessage());
        }
    }

    /**
     * 提取 PDF 元数据
     */
    private Map<String, String> extractMetadata(PDDocument document) {
        Map<String, String> metadata = new HashMap<>();
        PDDocumentInformation info = document.getDocumentInformation();

        if (info != null) {
            putIfNotNull(metadata, "title", info.getTitle());
            putIfNotNull(metadata, "author", info.getAuthor());
            putIfNotNull(metadata, "subject", info.getSubject());
            putIfNotNull(metadata, "keywords", info.getKeywords());
            putIfNotNull(metadata, "creator", info.getCreator());
            putIfNotNull(metadata, "producer", info.getProducer());

            if (info.getCreationDate() != null) {
                metadata.put("creationDate", info.getCreationDate().getTime().toString());
            }
            if (info.getModificationDate() != null) {
                metadata.put("modificationDate", info.getModificationDate().getTime().toString());
            }
        }

        return metadata;
    }

    private void putIfNotNull(Map<String, String> map, String key, String value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
    }
}
