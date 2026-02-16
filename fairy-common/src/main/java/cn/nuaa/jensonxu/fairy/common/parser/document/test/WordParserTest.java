package cn.nuaa.jensonxu.fairy.common.parser.document.test;

import cn.nuaa.jensonxu.fairy.common.parser.document.DocumentParseResult;
import cn.nuaa.jensonxu.fairy.common.parser.document.impl.WordDocumentParser;

import java.io.File;

/**
 * Word 解析器测试
 */
public class WordParserTest {

    public static void main(String[] args) {
        String wordFilePath = "C:\\Users\\15153\\Desktop\\文档\\课题组文档\\专业实践计划-徐梦飞.docx";
        System.out.println("========== Word 解析器测试 ==========\n");
        WordDocumentParser parser = new WordDocumentParser();
        File file = new File(wordFilePath);

        // 检查文件是否存在
        if (!file.exists()) {
            System.out.println("错误: 文件不存在 - " + wordFilePath);
            return;
        }

        // 检查是否支持该文件
        System.out.println("文件名: " + file.getName());
        System.out.println("文件大小: " + file.length() + " bytes");
        System.out.println("是否支持: " + parser.supports(file.getName()));
        System.out.println();

        // 执行解析
        System.out.println("开始解析...");
        DocumentParseResult result = parser.parse(file);

        // 输出结果
        System.out.println("\n========== 解析结果 ==========");
        System.out.println("解析状态: " + (result.isSuccess() ? "成功" : "失败"));
        System.out.println("文档类型: " + result.getContentType());
        System.out.println("是否加密: " + result.isEncrypted());
        System.out.println("段落数: " + result.getPageCount());
        System.out.println("字符数: " + result.getCharCount());
        System.out.println("解析耗时: " + result.getParseDuration() + " ms");

        if (result.getMetadata() != null && !result.getMetadata().isEmpty()) {
            System.out.println("\n---------- 元数据 ----------");
            result.getMetadata().forEach((key, value) ->
                    System.out.println(key + ": " + value));
        }

        if (result.isSuccess()) {
            System.out.println("\n---------- 文本内容预览 (前500字符) ----------");
            String content = result.getContent();
            if (content.length() > 500) {
                System.out.println(content.substring(0, 500) + "...");
            } else {
                System.out.println(content);
            }
        } else {
            System.out.println("\n错误信息: " + result.getErrorMessage());
        }

        System.out.println("\n========== 测试完成 ==========");
    }
}