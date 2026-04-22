package cn.nuaa.jensonxu.fairy.integration.service.mcp.config;

import lombok.Data;

/**
 * 第三方 MCP SSE 连接的配置项
 */
@Data
public class McpConnectionConfig {

    /**
     * 连接名称（对应 Nacos 配置中的 key，如 zhipu-web-research）
     */
    private String name;

    /**
     * 服务 URL
     */
    private String url;

    /**
     * SSE 端点路径
     */
    private String sseEndpoint;

    /**
     * 是否启用，false 时不建立连接
     */
    private boolean enabled = true;
}
