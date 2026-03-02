package cn.nuaa.jensonxu.fairy.integration.service.rag.chunker;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 文档分块器统一接口
 * 支持纯文本和图文混合文档的分块处理
 */
public interface DocumentChunker {

    /**
     * 获取分块配置
     */
    ChunkerConfig getConfig();

    /**
     * 设置分块配置
     */
    void setConfig(ChunkerConfig config);

    /**
     * 对文档列表进行分块
     *
     * @param documents 原始文档列表
     * @return 分块后的文档列表
     */
    List<Document> chunk(List<Document> documents);

    /**
     * 对单个文档进行分块
     *
     * @param document 原始文档
     * @return 分块后的文档列表
     */
    List<Document> chunkSingle(Document document);

    /**
     * 判断是否支持指定文档
     *
     * @param document 文档
     * @return 是否支持
     */
    boolean supports(Document document);
}
