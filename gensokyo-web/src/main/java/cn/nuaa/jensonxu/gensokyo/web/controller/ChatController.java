package cn.nuaa.jensonxu.gensokyo.web.controller;

import cn.nuaa.jensonxu.gensokyo.service.chat.ChatService;
import cn.nuaa.jensonxu.gensokyo.service.chat.data.CustomChatDTO;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody CustomChatDTO chatReq) {
        return Mono.fromCallable(() -> chatService.streamChat(chatReq))
                .flatMapMany(flux -> flux)
                .map(chunk -> "data: " + chunk + "\n\n")
                .subscribeOn(Schedulers.boundedElastic());
    }
}
