package cn.nuaa.jensonxu.fairy.common.file;

import cn.nuaa.jensonxu.fairy.common.parser.document.DocumentParseResult;
import cn.nuaa.jensonxu.fairy.common.parser.document.DocumentParserFactory;
import cn.nuaa.jensonxu.fairy.common.parser.image.ImageParseResult;
import cn.nuaa.jensonxu.fairy.common.parser.image.ImageParser;

import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;

/**
 * 文件处理器
 * 根据文件类型自动路由到文档解析器或图片解析器
 */
@Slf4j
public class FileProcessor {

    /**
     * 处理文件
     *
     * @param inputStream 文件输入流
     * @param fileName    文件名（用于判断类型）
     * @return 统一处理结果
     */
    public static FileProcessResult process(InputStream inputStream, String fileName) {
        if (inputStream == null) {
            return FileProcessResult.failure(fileName, "输入流为空");
        }
        if (fileName == null || fileName.isBlank()) {
            return FileProcessResult.failure(fileName, "文件名为空");
        }

        // 判断是否为图片
        if (ImageParser.isImage(fileName)) {
            return processImage(inputStream, fileName);
        }
        // 判断是否为支持的文档
        if (DocumentParserFactory.isSupported(fileName)) {
            return processDocument(inputStream, fileName);
        }

        log.warn("[file] 不支持的文件格式: {}", fileName);
        return FileProcessResult.failure(fileName, "不支持的文件格式: " + fileName);
    }

    /**
     * 判断文件是否受支持
     */
    public static boolean isSupported(String fileName) {
        return ImageParser.isImage(fileName) || DocumentParserFactory.isSupported(fileName);
    }

    /**
     * 获取文件类型
     */
    public static FileType getFileType(String fileName) {
        if (ImageParser.isImage(fileName)) {
            return FileType.IMAGE;
        }
        if (DocumentParserFactory.isSupported(fileName)) {
            return FileType.DOCUMENT;
        }
        return FileType.UNSUPPORTED;
    }

    private static FileProcessResult processImage(InputStream inputStream, String fileName) {
        log.info("[file] 图片文件，使用 ImageParser 处理: {}", fileName);
        ImageParseResult result = ImageParser.parse(inputStream, fileName);

        if (!result.isSuccess()) {
            return FileProcessResult.failure(fileName, result.getErrorMessage());
        }

        return FileProcessResult.builder()
                .success(true)
                .fileType(FileType.IMAGE)
                .base64Content(result.getBase64Content())
                .mimeType(result.getMimeType())
                .fileName(fileName)
                .parseDuration(result.getParseDuration())
                .build();
    }

    private static FileProcessResult processDocument(InputStream inputStream, String fileName) {
        log.info("[file] 文档文件，使用 DocumentParserFactory 处理: {}", fileName);
        DocumentParseResult result = DocumentParserFactory.parse(inputStream, fileName);

        if (!result.isSuccess()) {
            return FileProcessResult.failure(fileName, result.getErrorMessage());
        }

        return FileProcessResult.builder()
                .success(true)
                .fileType(FileType.DOCUMENT)
                .textContent(result.getContent())
                .fileName(fileName)
                .parseDuration(result.getParseDuration())
                .build();
    }
}

