package cn.nuaa.jensonxu.fairy.integration.mcp;

import cn.nuaa.jensonxu.fairy.integration.mcp.service.McpToolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
public class MCPRegister {

    @Bean
    public ToolCallbackProvider allTools(List<McpToolService> toolServices) {
        log.info("[nacos] 开始注册 MCP 工具");

        toolServices.forEach(service ->
                log.info("[nacos] 注册 MCP 工具: {}", service.getClass().getSimpleName())
        );

        log.info("[nacos] 共注册 {} 个 MCP 工具至 nacos", toolServices.size());

        return MethodToolCallbackProvider.builder()
                .toolObjects(toolServices.toArray())
                .build();
    }
}
