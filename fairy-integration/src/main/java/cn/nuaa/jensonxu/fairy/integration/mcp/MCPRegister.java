package cn.nuaa.jensonxu.fairy.integration.mcp;

import cn.nuaa.jensonxu.fairy.integration.mcp.service.PaperSearchService;
import cn.nuaa.jensonxu.fairy.integration.mcp.service.TimeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MCPRegister {

    @Bean
    public ToolCallbackProvider allTools(TimeService timeService,
                                         PaperSearchService paperSearchService) {
        log.info("[nacos] 注册 MCP 工具至 nacos");
        return MethodToolCallbackProvider.builder()
                .toolObjects(timeService,
                             paperSearchService)
                .build();
    }
}
