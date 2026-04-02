package cn.nuaa.jensonxu.fairy.common.data.llm;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgentEventDTO {

    /**
     * 事件类型，与 SSE 的 event 字段对应
     * 取值参考 AgentSseEventType 中定义的常量
     */
    private String eventType;

    /**
     * 事件携带的主要文字内容
     * - agent_start：当前使用的模型名称
     * - agent_thinking：LLM 当前轮推理文本
     * - agent_answer：最终答案的流式分块文本
     * - agent_end：总迭代次数（转为字符串）
     * - agent_error：错误信息
     */
    private String content;

    /**
     * 被调用工具的名称
     * 仅在 agent_tool_call 和 agent_tool_result 事件中有值
     */
    private String toolName;

    /**
     * 工具调用的入参（JSON 字符串）
     * 仅在 agent_tool_call 事件中有值
     */
    private String toolInput;

    /**
     * 工具执行的返回结果
     * 仅在 agent_tool_result 事件中有值
     */
    private String toolResult;

    /** Agent 会话 ID，所有事件均携带，便于前端按会话聚合展示 */
    private String agentSessionId;

    /**
     * 当前 ReAct 迭代轮次，从 1 开始
     * 在 agent_thinking / agent_tool_call / agent_tool_result 事件中有值
     */
    private Integer iterationIndex;

    /** 事件时间戳（毫秒） */
    private Long timestamp;

    /** 快速构建仅含 content 的事件（用于 agent_answer 分块推送） */
    public static AgentEventDTO ofContent(String eventType, String content, String agentSessionId) {
        return AgentEventDTO.builder()
                .eventType(eventType)
                .content(content)
                .agentSessionId(agentSessionId)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /** 快速构建工具调用事件（agent_tool_call） */
    public static AgentEventDTO ofToolCall(String toolName, String toolInput,
                                           String agentSessionId, int iterationIndex) {
        return AgentEventDTO.builder()
                .eventType("agent_tool_call")
                .toolName(toolName)
                .toolInput(toolInput)
                .agentSessionId(agentSessionId)
                .iterationIndex(iterationIndex)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /** 快速构建工具结果事件（agent_tool_result） */
    public static AgentEventDTO ofToolResult(String toolName, String toolResult,
                                             String agentSessionId, int iterationIndex) {
        return AgentEventDTO.builder()
                .eventType("agent_tool_result")
                .toolName(toolName)
                .toolResult(toolResult)
                .agentSessionId(agentSessionId)
                .iterationIndex(iterationIndex)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /** 快速构建技能加载事件（agent_skill_load） */
    public static AgentEventDTO ofSkillLoad(String skillName, String agentSessionId, int iterationIndex) {
        return AgentEventDTO.builder()
                .eventType("agent_skill_load")
                .content(skillName)
                .agentSessionId(agentSessionId)
                .iterationIndex(iterationIndex)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}

