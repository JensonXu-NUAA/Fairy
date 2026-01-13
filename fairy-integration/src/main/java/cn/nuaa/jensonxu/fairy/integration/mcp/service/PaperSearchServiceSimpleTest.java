package cn.nuaa.jensonxu.fairy.integration.mcp.service;

/**
 * 简单的Main方法测试类
 * 不依赖Spring容器,可以直接运行测试MCP工具
 */
public class PaperSearchServiceSimpleTest {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("开始测试 PaperSearchService");
        System.out.println("========================================\n");

        // 创建服务实例
        PaperSearchService service = new PaperSearchService();

        // 测试1: 按关键词搜索
        System.out.println("【测试1】按关键词搜索论文");
        System.out.println("----------------------------------------");
        String result1 = service.searchPapers("machine learning", 3);
        System.out.println(result1);
        System.out.println("\n");

        // 测试2: 按作者搜索
        System.out.println("【测试2】按作者搜索论文");
        System.out.println("----------------------------------------");
        String result2 = service.searchPapersByAuthor("Geoffrey Hinton", 3);
        System.out.println(result2);
        System.out.println("\n");

        // 测试3: 按arXiv ID获取论文详情
        System.out.println("【测试3】按arXiv ID获取论文详情");
        System.out.println("----------------------------------------");
        System.out.println("查询论文: Attention Is All You Need");
        String result3 = service.getPaperById("1706.03762");
        System.out.println(result3);
        System.out.println("\n");

        // 测试4: 测试错误处理 - 不存在的论文
        System.out.println("【测试4】测试错误处理 - 查询不存在的论文");
        System.out.println("----------------------------------------");
        String result4 = service.searchPapers("xyzabcnonexistent123", 5);
        System.out.println(result4);
        System.out.println("\n");

        // 测试5: 测试maxResults为null的情况
        System.out.println("【测试5】测试默认返回数量");
        System.out.println("----------------------------------------");
        String result5 = service.searchPapers("deep learning", null);
        System.out.println(result5);
        System.out.println("\n");

        System.out.println("========================================");
        System.out.println("所有测试完成!");
        System.out.println("========================================");
    }
}
