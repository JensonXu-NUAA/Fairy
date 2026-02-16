package cn.nuaa.jensonxu.fairy.common.parser.document;

import java.io.File;
import java.io.InputStream;

/**
 * 文档解析器接口
 */
public interface DocumentParser {

    /**
     * 解析文件
     *
     * @param file 文件对象
     * @return 解析结果
     */
    DocumentParseResult parse(File file);

    /**
     * 解析输入流
     *
     * @param inputStream 输入流
     * @param fileName    文件名（用于判断文件类型）
     * @return 解析结果
     */
    DocumentParseResult parse(InputStream inputStream, String fileName);

    /**
     * 解析加密文档
     *
     * @param file     文件对象
     * @param password 密码
     * @return 解析结果
     */
    DocumentParseResult parseWithPassword(File file, String password);

    /**
     * 解析加密文档（输入流）
     *
     * @param inputStream 输入流
     * @param fileName    文件名
     * @param password    密码
     * @return 解析结果
     */
    DocumentParseResult parseWithPassword(InputStream inputStream, String fileName, String password);

    /**
     * 判断是否支持该文件类型
     *
     * @param fileName 文件名
     * @return 是否支持
     */
    boolean supports(String fileName);

    /**
     * 获取支持的文件扩展名列表
     *
     * @return 扩展名数组（如：[".pdf"]、[".docx", ".doc"]）
     */
    String[] getSupportedExtensions();
}

