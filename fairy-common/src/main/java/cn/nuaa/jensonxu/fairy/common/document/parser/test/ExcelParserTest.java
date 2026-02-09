package cn.nuaa.jensonxu.fairy.common.document.parser.test;

import cn.nuaa.jensonxu.fairy.common.document.parser.DocumentParseResult;
import cn.nuaa.jensonxu.fairy.common.document.parser.impl.ExcelDocumentParser;

import java.io.File;

/**
 * Excel 解析器测试
 */
public class ExcelParserTest {

    public static void main(String[] args) {
        String excelFilePath = "C:\\Users\\15153\\Desktop\\文档\\各种材料\\徐梦飞-SZ2316035-硕士特别奖学金\\附件C-申请汇总表-徐梦飞.xls";
        System.out.println("========== Excel 解析器测试 ==========\n");
        ExcelDocumentParser parser = new ExcelDocumentParser();
        File file = new File(excelFilePath);

        // 检查文件是否存在
        if (!file.exists()) {
            System.out.println("错误: 文件不存在 - " + excelFilePath);
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
        System.out.println("工作表数: " + result.getPageCount());
        System.out.println("字符数: " + result.getCharCount());
        System.out.println("解析耗时: " + result.getParseDuration() + " ms");

        if (result.getMetadata() != null && !result.getMetadata().isEmpty()) {
            System.out.println("\n---------- 元数据 ----------");
            result.getMetadata().forEach((key, value) ->
                    System.out.println(key + ": " + value));
        }

        if (result.isSuccess()) {
            System.out.println("\n---------- 文本内容预览 (前800字符) ----------");
            String content = result.getContent();
            if (content.length() > 800) {
                System.out.println(content.substring(0, 800) + "...");
            } else {
                System.out.println(content);
            }
        } else {
            System.out.println("\n错误信息: " + result.getErrorMessage());
        }

        System.out.println("\n========== 测试完成 ==========");
    }
}
