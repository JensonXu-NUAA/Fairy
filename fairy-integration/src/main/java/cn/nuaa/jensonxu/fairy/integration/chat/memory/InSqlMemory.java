package cn.nuaa.jensonxu.fairy.integration.chat.memory;

import cn.nuaa.jensonxu.fairy.common.repository.mysql.CustomChatMemoryRepository;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.data.CustomChatMessageDO;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class InSqlMemory implements ChatMemory {

    private final CustomChatMemoryRepository repository;

    @Autowired
    public InSqlMemory(CustomChatMemoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void add(@NotNull String conversationId, @NotNull Message message) {
        Map<String, Object> properties = message.getMetadata();
        CustomChatMessageDO customChatMessageDO = new CustomChatMessageDO();

        customChatMessageDO.setId(null);
        customChatMessageDO.setConversationId(conversationId);
        customChatMessageDO.setType(message.getMessageType().getValue());
        customChatMessageDO.setChatId(UUID.randomUUID().toString());
        customChatMessageDO.setUserId(properties.get("userId").toString());
        customChatMessageDO.setModelName(properties.get("modelName").toString());
        customChatMessageDO.setMessage(message.getText());
        customChatMessageDO.setCreateTime(new Date(System.currentTimeMillis()));

        repository.insert(customChatMessageDO);
    }

    @Override
    public void add(@NotNull String conversationId, @NotNull List<Message> messages) {

    }

    @NotNull
    @Override
    public List<Message> get(@NotNull String conversationId) {
        List<CustomChatMessageDO> customChatMessageDOList = repository.selectByConversationId(conversationId);
        if (CollectionUtils.isNotEmpty(customChatMessageDOList)) {
            return customChatMessageDOList.stream()
                    .map(messageDO -> {
                        Map<String, Object> metadata = Map.of(
                                "userId", messageDO.getUserId(),
                                "chatId", messageDO.getChatId(),
                                "conversationId", messageDO.getConversationId(),
                                "modelName", messageDO.getModelName(),
                                "createTime", messageDO.getCreateTime(),
                                "id", messageDO.getId()
                        );

                        MessageType type = MessageType.fromValue(messageDO.getType());

                        // 根据类型返回相应的Spring AI标准消息类型
                        return switch (type) {
                            case USER -> UserMessage.builder()
                                    .text(messageDO.getMessage())
                                    .metadata(metadata)
                                    .build();
                            case ASSISTANT -> AssistantMessage.builder()
                                    .content(messageDO.getMessage())
                                    .properties(metadata)
                                    .build();
                            case SYSTEM -> SystemMessage.builder()
                                    .text(messageDO.getMessage())
                                    .metadata(metadata)
                                    .build();
                            default -> throw new IllegalArgumentException("Unsupported message type: " + type);
                        };
                    })
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }

    }

    @Override
    public void clear(@NotNull String conversationId) {

    }
}
