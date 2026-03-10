package cn.nuaa.jensonxu.fairy.service.agent;

import cn.nuaa.jensonxu.fairy.common.data.llm.AgentChatDTO;
import cn.nuaa.jensonxu.fairy.integration.agent.AgentClientBuilder;
import cn.nuaa.jensonxu.fairy.integration.agent.AgentProperties;
import cn.nuaa.jensonxu.fairy.integration.agent.handler.AgentHandler;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentClientBuilder agentClientBuilder;
    private final AgentProperties agentProperties;

    /**
     * Agent 流式对话入口
     * 创建 SseEmitter 后立即返回，实际执行由 AgentHandler 异步驱动
     *
     * @param agentChatDTO 请求参数（含用户消息、模型名称、agentSessionId 等）
     * @return SseEmitter，HTTP 层持有此对象向客户端推送 Agent 执行事件
     */
    public SseEmitter chat(AgentChatDTO agentChatDTO) {

        String agentSessionId = agentChatDTO.getAgentSessionId();  // 确保 agentSessionId 已生成
        int maxIterations = resolveMaxIterations(agentChatDTO.getMaxIterations());  // 解析最终使用的 maxIterations
        log.info("[agent] 开始 Agent 对话 - userId: {}, agentSessionId: {}, model: {}, maxIterations: {}", agentChatDTO.getUserId(), agentSessionId, agentChatDTO.getModelName(), maxIterations);
        ReactAgent reactAgent = agentClientBuilder.build(agentChatDTO.getModelName());  // 构建 ReactAgent（含模型 + 工具）

        // 创建 SSE 连接（0L 表示不超时，由 Agent 执行完毕后主动关闭）
        SseEmitter sseEmitter = new SseEmitter(0L);
        setSseCallbacks(sseEmitter, agentSessionId);

        // 实例化 AgentHandler，异步执行 Agent 推理循环
        AgentHandler agentHandler = new AgentHandler(reactAgent, sseEmitter, agentChatDTO, agentProperties, maxIterations);
        agentHandler.run();

        return sseEmitter;
    }

    /**
     * 解析最终使用的 maxIterations
     * 取请求值与全局上限的较小值，防止客户端传入过大的值
     */
    private int resolveMaxIterations(Integer requested) {
        int globalMax = agentProperties.getMaxIterations();
        if (requested == null || requested <= 0) {
            return globalMax;
        }
        return Math.min(requested, globalMax);
    }

    /**
     * 设置 SSE 生命周期回调，与 ChatService 保持一致的日志风格
     */
    private void setSseCallbacks(SseEmitter emitter, String agentSessionId) {
        emitter.onCompletion(() ->
                log.info("[agent] SSE 连接完成 - agentSessionId: {}", agentSessionId));

        emitter.onTimeout(() -> {
            log.warn("[agent] SSE 连接超时 - agentSessionId: {}", agentSessionId);
            emitter.complete();
        });

        emitter.onError(e ->
                log.error("[agent] SSE 连接错误 - agentSessionId: {}, 错误: {}", agentSessionId, e.getMessage()));
    }
}