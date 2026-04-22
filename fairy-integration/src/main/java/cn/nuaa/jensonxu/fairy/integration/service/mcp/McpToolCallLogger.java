package cn.nuaa.jensonxu.fairy.integration.service.mcp;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * 第三方 MCP 工具调用日志包装器
 * 在工具调用前后打印工具名称、入参、出参，便于调试和追踪
 */
@Slf4j
public class McpToolCallLogger implements ToolCallback {

    private final ToolCallback delegate;

    public McpToolCallLogger(ToolCallback delegate) {
        this.delegate = delegate;
    }

    @Override
    public @NonNull ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public @NonNull String call(@NonNull String toolInput) {
        String toolName = delegate.getToolDefinition().name();
        log.info("[mcp] >>> 调用: {}, 入参: {}", toolName, toolInput);
        String result = delegate.call(toolInput);
        log.info("[mcp] <<< 返回: {}, 结果长度: {} 字符", toolName, result.length());
        return result;
    }
}