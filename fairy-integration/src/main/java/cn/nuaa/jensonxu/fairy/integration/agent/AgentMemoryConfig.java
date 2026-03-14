package cn.nuaa.jensonxu.fairy.integration.agent;

import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentMemoryConfig {

    /**
     * 共享 MemorySaver，作为 ReactAgent 的运行时载体
     * 按 threadId（agentSessionId）隔离各会话的状态
     * 服务重启后由 AgentClientBuilder 从 Redis/MySQL 回填历史消息
     */
    @Bean
    public MemorySaver agentMemorySaver() {
        return new MemorySaver();
    }
}
