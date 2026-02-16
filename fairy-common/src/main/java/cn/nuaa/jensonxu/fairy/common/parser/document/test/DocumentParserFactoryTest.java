package cn.nuaa.jensonxu.fairy.common.parser.document.test;

import cn.nuaa.jensonxu.fairy.common.parser.document.DocumentParseResult;
import cn.nuaa.jensonxu.fairy.common.parser.document.DocumentParser;
import cn.nuaa.jensonxu.fairy.common.parser.document.DocumentParserFactory;

import java.io.File;
import java.util.List;

/**
 * 解析器工厂测试
 */
public class DocumentParserFactoryTest {

    public static void main(String[] args) {
        System.out.println("========== 解析器工厂测试 ==========\n");
        testParserSelection();  // 1. 测试解析器自动选择
        testSupportedExtensions();  // 2. 测试支持的扩展名列表
        testFileParsing();  // 3. 测试实际文件解析（可替换为您的测试文件）
        System.out.println("\n========== 测试完成 ==========");
    }

    /**
     * 测试解析器自动选择
     */
    private static void testParserSelection() {
        System.out.println("---------- 解析器自动选择测试 ----------");

        String[] testFiles = {
                "document.pdf",
                "document.doc",
                "document.docx",
                "spreadsheet.xls",
                "spreadsheet.xlsx",
                "presentation.ppt",
                "presentation.pptx",
                "code.java",
                "script.py",
                "config.json",
                "readme.md",
                "data.txt",
                "image.png",      // 不支持
                "unknown.xyz"     // 不支持
        };

        for (String fileName : testFiles) {
            DocumentParser parser = DocumentParserFactory.getParser(fileName);
            String parserName = parser != null ? parser.getClass().getSimpleName() : "不支持";
            boolean supported = DocumentParserFactory.isSupported(fileName);
            System.out.printf("%-25s -> %-25s (支持: %s)%n", fileName, parserName, supported);
        }
        System.out.println();
    }

    /**
     * 测试支持的扩展名列表
     */
    private static void testSupportedExtensions() {
        System.out.println("---------- 支持的扩展名列表 ----------");
        List<String> extensions = DocumentParserFactory.getAllSupportedExtensions();
        System.out.println("共支持 " + extensions.size() + " 种文件格式：");

        // 按行显示，每行10个
        for (int i = 0; i < extensions.size(); i++) {
            System.out.print(extensions.get(i));
            if ((i + 1) % 10 == 0) {
                System.out.println();
            } else {
                System.out.print("  ");
            }
        }
        System.out.println("\n");
    }

    /**
     * 测试实际文件解析
     */
    private static void testFileParsing() {
        System.out.println("---------- 实际文件解析测试 ----------");

        // TODO: 替换为实际测试文件路径
        String[] testFilePaths = {
                "C:\\Users\\15153\\Downloads\\offer.pdf",
                "C:\\Users\\15153\\Desktop\\文档\\课题组文档\\专业实践计划-徐梦飞.docx",
                "C:\\Users\\15153\\Desktop\\文档\\各种材料\\徐梦飞-SZ2316035-硕士特别奖学金\\附件C-申请汇总表-徐梦飞.xlsx",
                "C:\\Users\\15153\\Desktop\\新建 文本文档.txt",
                "D:\\Fairy\\fairy-common\\src\\main\\java\\cn\\nuaa\\jensonxu\\fairy\\common\\repository\\caffeine\\CaffeineConfig.java"
        };

        for (String filePath : testFilePaths) {
            File file = new File(filePath);

            System.out.println("\n文件: " + file.getName());

            if (!file.exists()) {
                System.out.println("  状态: 文件不存在，跳过");
                continue;
            }

            // 使用工厂解析
            DocumentParseResult result = DocumentParserFactory.parse(file);

            System.out.println("  解析状态: " + (result.isSuccess() ? "成功" : "失败"));
            System.out.println("  文档类型: " + result.getContentType());
            System.out.println("  字符数: " + result.getCharCount());
            System.out.println("  解析耗时: " + result.getParseDuration() + " ms");

            if (!result.isSuccess()) {
                System.out.println("  错误信息: " + result.getErrorMessage());
            }
        }
    }
}