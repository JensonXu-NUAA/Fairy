package cn.nuaa.jensonxu.fairy.integration.agent;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent 全局配置属性
 * 绑定 application.yml 中 fairy.agent.* 配置项
 */
@Data
@Component
@ConfigurationProperties(prefix = "fairy.agent")
public class AgentProperties {

    /**
     * 全局默认模型名称
     * 请求中 modelName 为空时使用此值
     */
    private String defaultModel = "qwen-turbo";

    /**
     * ReAct 最大循环迭代次数上限
     */
    private int maxIterations = 10;

    /**
     * 是否将 LLM 推理过程（Thought）推送给客户端
     */
    private boolean streamThinking = true;

    /**
     * Agent 会话历史保留的最大轮次
     */
    private int maxHistorySize = 10;

    /**
     * 记忆管理配置
     */
    private Memory memory = new Memory();

    @Data
    public static class Memory {

        private ShortTerm shortTerm = new ShortTerm();
        private LongTerm longTerm = new LongTerm();

        @Data
        public static class ShortTerm {

            /** Redis 滑动窗口大小（条） */
            private int maxMessages = 20;

            /** Redis key 过期时间（小时） */
            private int ttlHours = 24;
        }

        @Data
        public static class LongTerm {

            /** 触发摘要的消息数阈值 */
            private int summarizeThreshold = 20;

            /** MySQL 中每用户保留的最大记忆条数 */
            private int maxFactsPerUser = 50;

            /** 摘要专用模型名称 */
            private String modelName = "qwen-flash";
        }
    }
}