package cn.nuaa.jensonxu.gensokyo.web.controller;

import cn.nuaa.jensonxu.gensokyo.service.chat.ChatService;
import cn.nuaa.jensonxu.gensokyo.integration.chat.data.CustomChatDTO;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody CustomChatDTO chatReq) {
        log.info("收到流式聊天请求 - UserID: {}", chatReq);
        return chatService.streamChat(chatReq);
    }
}
