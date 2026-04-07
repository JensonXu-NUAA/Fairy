package cn.nuaa.jensonxu.fairy.common.data.llm;

/**
 * Agent SSE 事件类型常量
 * 与现有对话链路的 SSE_START / SSE_MESSAGE / SSE_END 并列，
 * 通过 event 字段区分普通对话事件和 Agent 事件
 */
public final class AgentSseEventType {

    private AgentSseEventType() {}

    /** Agent 任务启动，携带 agentSessionId、userId、modelName */
    public static final String AGENT_START = "agent_start";

    /**
     * LLM 当前轮推理内容（Thought 部分）
     * 仅在 AgentProperties.streamThinking = true 时下发
     */
    public static final String AGENT_THINKING = "agent_thinking";

    /** LLM 决定调用某个工具，携带 toolName 和 toolInput */
    public static final String AGENT_TOOL_CALL = "agent_tool_call";

    /** 工具执行完毕，携带 toolName 和 toolResult */
    public static final String AGENT_TOOL_RESULT = "agent_tool_result";

    /**
     * LLM 给出最终答案，分块流式下发
     * 行为与现有 SSE_MESSAGE 一致，但 event 名不同以便前端区分来源
     */
    public static final String AGENT_ANSWER = "agent_answer";

    /** Agent 任务完成，携带总迭代次数和本次使用的工具列表 */
    public static final String AGENT_END = "agent_end";

    /** 发生不可恢复错误时下发，携带错误信息 */
    public static final String AGENT_ERROR = "agent_error";

    /** 流结束标记，与现有 SSE_DONE 保持一致 */
    public static final String DONE = "[DONE]";

    /** 模型识别到需要加载某个 Skill，携带 skillName */
    public static final String AGENT_SKILL_LOAD = "agent_skill_load";

}
