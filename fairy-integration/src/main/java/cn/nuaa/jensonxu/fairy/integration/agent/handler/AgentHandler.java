package cn.nuaa.jensonxu.fairy.integration.agent.handler;

import cn.nuaa.jensonxu.fairy.common.data.llm.AgentChatDTO;
import cn.nuaa.jensonxu.fairy.common.data.llm.AgentEventDTO;
import cn.nuaa.jensonxu.fairy.integration.agent.AgentProperties;
import cn.nuaa.jensonxu.fairy.integration.agent.AgentSseEventType;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;

import com.alibaba.fastjson2.JSON;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

/**
 * Agent 执行处理器
 * 负责驱动 ReactAgent 流式执行，并将各阶段结果映射为 SSE 事件推送给客户端
 */
@Slf4j
public class AgentHandler {

    private final ReactAgent reactAgent;
    private final SseEmitter sseEmitter;
    private final AgentChatDTO agentChatDTO;
    private final AgentProperties agentProperties;

    /** SSE 数据块序号，与 BaseChatModelClientHandler 保持一致的格式 */
    private Integer chunkId = 1;

    public AgentHandler(ReactAgent reactAgent, SseEmitter sseEmitter, AgentChatDTO agentChatDTO, AgentProperties agentProperties, int maxIterations) {
        this.reactAgent = reactAgent;
        this.sseEmitter = sseEmitter;
        this.agentChatDTO = agentChatDTO;
        this.agentProperties = agentProperties;
    }

    /**
     * 异步执行入口，由 AgentService 调用后立即返回
     * 实际推理和 SSE 推送在独立线程中进行
     */
    public void run() {
        CompletableFuture.runAsync(() -> {
            try {
                sendStart();
                executeAgentStream();
            } catch (Exception e) {
                handleError(e);
            }
        });
    }

    /**
     * 订阅 ReactAgent 流式输出，阻塞至流结束后发送结束事件
     */
    private void executeAgentStream() {
        try {
            reactAgent.stream(agentChatDTO.getMessage()).doOnNext(output -> {
                        try {
                            handleNodeOutput(output);
                        } catch (Exception e) {
                            log.error("[agent] 处理节点输出异常 - agentSessionId: {}",
                                    agentChatDTO.getAgentSessionId(), e);
                        }
                    })
                    .blockLast();
            sendEnd();  // 流正常结束
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * 按 OutputType 将 NodeOutput 映射到对应的 SSE 事件
     */
    private void handleNodeOutput(NodeOutput output) throws Exception {
        if (!(output instanceof StreamingOutput streamingOutput)) {
            return;
        }

        OutputType type = streamingOutput.getOutputType();
        var message = streamingOutput.message();

        switch (type) {
            case AGENT_MODEL_STREAMING -> {
                if (message instanceof AssistantMessage assistantMessage) {
                    Object reasoningContent = assistantMessage.getMetadata().get("reasoningContent");
                    if (reasoningContent != null && StringUtils.isNotBlank(reasoningContent.toString())) {
                        // 推理过程（Thought），受 streamThinking 开关控制
                        if (agentProperties.isStreamThinking()) {
                            AgentEventDTO event = AgentEventDTO.ofContent(AgentSseEventType.AGENT_THINKING, reasoningContent.toString(), agentChatDTO.getAgentSessionId());
                            sendSseEvent(AgentSseEventType.AGENT_THINKING, JSON.toJSONString(event));
                        }
                    } else {
                        String text = assistantMessage.getText();  // 最终答案分块，流式下发
                        if (StringUtils.isNotBlank(text)) {
                            AgentEventDTO event = AgentEventDTO.ofContent(AgentSseEventType.AGENT_ANSWER, text, agentChatDTO.getAgentSessionId());
                            sendSseEvent(AgentSseEventType.AGENT_ANSWER, JSON.toJSONString(event));
                        }
                    }
                }
            }

            case AGENT_MODEL_FINISHED -> {
                // 模型推理完成，若包含工具调用则发送 AGENT_TOOL_CALL 事件
                if (message instanceof AssistantMessage assistantMessage && assistantMessage.hasToolCalls()) {
                    for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
                        AgentEventDTO event = AgentEventDTO.ofToolCall(toolCall.name(), toolCall.arguments(), agentChatDTO.getAgentSessionId(), chunkId);
                        sendSseEvent(AgentSseEventType.AGENT_TOOL_CALL, JSON.toJSONString(event));
                    }
                }
            }

            case AGENT_TOOL_FINISHED -> {
                // 工具执行完毕，发送 AGENT_TOOL_RESULT 事件
                if (message instanceof ToolResponseMessage toolResponse) {
                    for (ToolResponseMessage.ToolResponse response : toolResponse.getResponses()) {
                        AgentEventDTO event = AgentEventDTO.ofToolResult(response.name(), response.responseData(), agentChatDTO.getAgentSessionId(), chunkId);
                        sendSseEvent(AgentSseEventType.AGENT_TOOL_RESULT, JSON.toJSONString(event));
                    }
                }
            }

            default ->
                    log.debug("[agent] 忽略节点输出类型: {}, node: {}", type, streamingOutput.node());
        }
    }

    private void sendStart() throws Exception {
        AgentEventDTO event = AgentEventDTO.ofContent(AgentSseEventType.AGENT_START, agentChatDTO.getModelName(), agentChatDTO.getAgentSessionId());
        sendSseEvent(AgentSseEventType.AGENT_START, JSON.toJSONString(event));
    }

    private void sendEnd() {
        try {
            AgentEventDTO event = AgentEventDTO.ofContent(AgentSseEventType.AGENT_END, null, agentChatDTO.getAgentSessionId());
            sendSseEvent(AgentSseEventType.AGENT_END, JSON.toJSONString(event));
            sendSseEvent(AgentSseEventType.DONE, AgentSseEventType.DONE);
            sseEmitter.complete();
        } catch (Exception e) {
            log.error("[agent] 发送结束事件异常 - agentSessionId: {}", agentChatDTO.getAgentSessionId(), e);
            sseEmitter.completeWithError(e);
        }
    }

    private void handleError(Throwable e) {
        log.error("[agent] 执行异常 - agentSessionId: {}", agentChatDTO.getAgentSessionId(), e);
        try {
            AgentEventDTO event = AgentEventDTO.ofContent(AgentSseEventType.AGENT_ERROR, e.getMessage(), agentChatDTO.getAgentSessionId());
            sendSseEvent(AgentSseEventType.AGENT_ERROR, JSON.toJSONString(event));
        } catch (Exception ex) {
            log.error("[agent] 发送错误事件异常", ex);
        } finally {
            sseEmitter.completeWithError(e);
        }
    }

    /**
     * SSE 事件发送
     */
    private void sendSseEvent(String event, String data) throws Exception {
        SseEmitter.SseEventBuilder builder = SseEmitter.event()
                .id(chunkId.toString())
                .name(event)
                .data(data)
                .reconnectTime(3000L);
        sseEmitter.send(builder.build());
        chunkId++;
    }
}