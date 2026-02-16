package cn.nuaa.jensonxu.fairy.common.parser.document.test;

import cn.nuaa.jensonxu.fairy.common.parser.document.DocumentParseResult;
import cn.nuaa.jensonxu.fairy.common.parser.document.impl.PdfDocumentParser;

import java.io.File;

/**
 * PDF 解析器测试
 */
public class PdfParserTest {

    public static void main(String[] args) {
        String pdfFilePath = "C:\\Users\\15153\\Downloads\\徐梦飞-录取通知书.pdf";
        System.out.println("========== PDF 解析器测试 ==========\n");
        PdfDocumentParser parser = new PdfDocumentParser();
        File file = new File(pdfFilePath);

        // 检查文件是否存在
        if (!file.exists()) {
            System.out.println("错误: 文件不存在 - " + pdfFilePath);
            return;
        }

        // 检查是否支持该文件
        System.out.println("文件名: " + file.getName());
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
        System.out.println("页数: " + result.getPageCount());
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

