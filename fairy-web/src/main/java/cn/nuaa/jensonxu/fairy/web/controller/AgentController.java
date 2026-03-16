package cn.nuaa.jensonxu.fairy.web.controller;

import cn.nuaa.jensonxu.fairy.common.data.llm.AgentChatDTO;
import cn.nuaa.jensonxu.fairy.integration.agent.memory.AgentMemorySummarizer;
import cn.nuaa.jensonxu.fairy.service.agent.AgentService;

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

    @GetMapping("/test-summarizer")
    public String testSummarizer() {
        List<Message> testMessages = List.of(
                new UserMessage("我叫张三，是一名 Java 后端开发工程师，正在开发一个叫 Fairy 的 Spring Boot 项目"),
                new AssistantMessage("好的，我了解了，Fairy 是一个基于 Spring Boot 的项目，您是后端开发工程师。")
        );
        List<AgentMemorySummarizer.SummaryItem> items = summarizer.summarize(testMessages, "test_user", "test_session");
        return items.toString();
    }

}