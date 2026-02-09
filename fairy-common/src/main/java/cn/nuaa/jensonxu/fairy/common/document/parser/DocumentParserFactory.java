package cn.nuaa.jensonxu.fairy.common.document.parser;

import cn.nuaa.jensonxu.fairy.common.document.parser.impl.ExcelDocumentParser;
import cn.nuaa.jensonxu.fairy.common.document.parser.impl.PdfDocumentParser;
import cn.nuaa.jensonxu.fairy.common.document.parser.impl.TikaDocumentParser;
import cn.nuaa.jensonxu.fairy.common.document.parser.impl.WordDocumentParser;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档解析器工厂
 * 根据文件类型自动选择合适的解析器
 */
@Slf4j
public class DocumentParserFactory {

    private static final List<DocumentParser> PARSERS = new ArrayList<>();

    static {
        PARSERS.add(new PdfDocumentParser());
        PARSERS.add(new WordDocumentParser());
        PARSERS.add(new ExcelDocumentParser());
        PARSERS.add(new TikaDocumentParser());
    }

    /**
     * 根据文件名获取合适的解析器
     *
     * @param fileName 文件名
     * @return 解析器，如果不支持则返回 null
     */
    public static DocumentParser getParser(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return null;
        }

        for (DocumentParser parser : PARSERS) {
            if (parser.supports(fileName)) {
                log.debug("[parser] 文件 [{}] 使用解析器: {}", fileName, parser.getClass().getSimpleName());
                return parser;
            }
        }

        log.warn("[parser] 未找到支持文件 [{}] 的解析器", fileName);
        return null;
    }

    /**
     * 解析文件
     *
     * @param file 文件对象
     * @return 解析结果
     */
    public static DocumentParseResult parse(File file) {
        if (file == null || !file.exists()) {
            return DocumentParseResult.failure("文件不存在");
        }

        DocumentParser parser = getParser(file.getName());
        if (parser == null) {
            return DocumentParseResult.failure("不支持的文件格式: " + file.getName());
        }

        return parser.parse(file);
    }

    /**
     * 解析输入流
     *
     * @param inputStream 输入流
     * @param fileName    文件名（用于判断类型）
     * @return 解析结果
     */
    public static DocumentParseResult parse(InputStream inputStream, String fileName) {
        if (inputStream == null) {
            return DocumentParseResult.failure("输入流为空");
        }

        DocumentParser parser = getParser(fileName);
        if (parser == null) {
            return DocumentParseResult.failure("不支持的文件格式: " + fileName);
        }

        return parser.parse(inputStream, fileName);
    }

    /**
     * 解析加密文件
     *
     * @param file     文件对象
     * @param password 密码
     * @return 解析结果
     */
    public static DocumentParseResult parseWithPassword(File file, String password) {
        if (file == null || !file.exists()) {
            return DocumentParseResult.failure("文件不存在");
        }

        DocumentParser parser = getParser(file.getName());
        if (parser == null) {
            return DocumentParseResult.failure("不支持的文件格式: " + file.getName());
        }

        return parser.parseWithPassword(file, password);
    }

    /**
     * 解析加密输入流
     *
     * @param inputStream 输入流
     * @param fileName    文件名
     * @param password    密码
     * @return 解析结果
     */
    public static DocumentParseResult parseWithPassword(InputStream inputStream, String fileName, String password) {
        if (inputStream == null) {
            return DocumentParseResult.failure("输入流为空");
        }

        DocumentParser parser = getParser(fileName);
        if (parser == null) {
            return DocumentParseResult.failure("不支持的文件格式: " + fileName);
        }

        return parser.parseWithPassword(inputStream, fileName, password);
    }

    /**
     * 判断是否支持该文件格式
     *
     * @param fileName 文件名
     * @return 是否支持
     */
    public static boolean isSupported(String fileName) {
        return getParser(fileName) != null;
    }

    /**
     * 获取所有支持的文件扩展名
     *
     * @return 扩展名列表
     */
    public static List<String> getAllSupportedExtensions() {
        List<String> extensions = new ArrayList<>();
        for (DocumentParser parser : PARSERS) {
            for (String ext : parser.getSupportedExtensions()) {
                if (!extensions.contains(ext)) {
                    extensions.add(ext);
                }
            }
        }
        return extensions;
    }
}
