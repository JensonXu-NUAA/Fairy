package cn.nuaa.jensonxu.fairy.web.controller;

import cn.nuaa.jensonxu.fairy.common.data.agent.AgentSessionVO;
import cn.nuaa.jensonxu.fairy.common.data.agent.AgentSessionMessageVO;
import cn.nuaa.jensonxu.fairy.common.data.agent.ModelVO;
import cn.nuaa.jensonxu.fairy.common.data.file.response.CustomResponse;
import cn.nuaa.jensonxu.fairy.integration.agent.model.manager.AgentModelManager;
import cn.nuaa.jensonxu.fairy.service.agent.AgentSessionQueryService;
import cn.nuaa.jensonxu.fairy.common.data.llm.AgentChatDTO;
import cn.nuaa.jensonxu.fairy.integration.agent.memory.AgentMemorySummarizer;
import cn.nuaa.jensonxu.fairy.service.agent.AgentService;

import org.apache.commons.lang3.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/agent")
public class AgentController {

    private final AgentService agentService;
    private final AgentMemorySummarizer summarizer;
    private final AgentModelManager agentModelManager;
    private final AgentSessionQueryService sessionQueryService;

    /**
     * Agent SSE 流式对话接口
     * 与现有 /chat/stream 并列，互不干扰
     * POST /agent/chat
     * Content-Type: application/json
     * Response: text/event-stream
     */
    @PostMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter agentChat(@RequestBody AgentChatDTO agentChatDTO) {
        log.info("[agent] 收到 Agent 对话请求 - userId: {}, model: {}", agentChatDTO.getUserId(), agentChatDTO.getModelName());
        return agentService.chat(agentChatDTO);
    }

    @GetMapping("/test-summarizer")
    public String testSummarizer() {
        List<Message> testMessages = List.of(
                new UserMessage("我叫张三，是一名 Java 后端开发工程师，正在开发一个叫 Fairy 的 Spring Boot 项目"),
                new AssistantMessage("好的，我了解了，Fairy 是一个基于 Spring Boot 的项目，您是后端开发工程师。")
        );
        List<AgentMemorySummarizer.SummaryItem> items = summarizer.summarize(testMessages, "test_user", "test_session");
        return items.toString();
    }

    /**
     * 查询用户会话列表
     * GET /agent/sessions?userId=xxx
     */
    @GetMapping("/sessions")
    public CustomResponse<List<AgentSessionVO>> listSessions(@RequestParam String userId) {
        if (StringUtils.isBlank(userId)) {
            return CustomResponse.error("userId 不能为空");
        }
        log.info("[agent] 查询会话列表 - userId: {}", userId);
        return CustomResponse.success(sessionQueryService.listSessions(userId));
    }

    /**
     * 查询某会话的完整消息记录
     * GET /agent/sessions/{sessionId}/messages
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public CustomResponse<List<AgentSessionMessageVO>> listMessages(@PathVariable String sessionId) {
        log.info("[agent] 查询会话消息 - sessionId: {}", sessionId);
        return CustomResponse.success(sessionQueryService.listMessages(sessionId));
    }

    /**
     * 查询当前可用模型列表
     * GET /agent/models
     */
    @GetMapping("/models")
    public CustomResponse<List<ModelVO>> listModels() {
        List<ModelVO> models = agentModelManager.getAvailableModels().stream()
                .map(m -> ModelVO.builder()
                        .modelName(m.getModelName())
                        .provider(m.getProvider())
                        .enableThinking(m.getParameters() != null
                                ? m.getParameters().getEnableThinking()
                                : null)
                        .build())
                .toList();
        return CustomResponse.success(models);
    }
}