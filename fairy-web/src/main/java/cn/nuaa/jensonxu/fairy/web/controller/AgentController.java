package cn.nuaa.jensonxu.fairy.web.controller;

import cn.nuaa.jensonxu.fairy.common.data.llm.AgentChatDTO;
import cn.nuaa.jensonxu.fairy.service.agent.AgentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/agent")
public class AgentController {

    private final AgentService agentService;

    /**
     * Agent SSE 流式对话接口
     * 与现有 /chat/stream 并列，互不干扰
     *
     * POST /agent/chat
     * Content-Type: application/json
     * Response: text/event-stream
     */
    @PostMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter agentChat(@RequestBody AgentChatDTO agentChatDTO) {
        log.info("[agent] 收到 Agent 对话请求 - userId: {}, model: {}", agentChatDTO.getUserId(), agentChatDTO.getModelName());
        return agentService.chat(agentChatDTO);
    }
}