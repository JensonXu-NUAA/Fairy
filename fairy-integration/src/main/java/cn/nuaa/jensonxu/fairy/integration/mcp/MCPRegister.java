package cn.nuaa.jensonxu.fairy.integration.mcp;

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
    public ToolCallbackProvider tools(TimeService timeService) {
        log.info("[nacos] 注册服务至nacos");
        return MethodToolCallbackProvider.builder()
                .toolObjects(timeService)
                .build();
    }
}
