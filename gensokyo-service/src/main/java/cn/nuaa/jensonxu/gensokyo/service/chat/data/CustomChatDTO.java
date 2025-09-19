package cn.nuaa.jensonxu.gensokyo.service.chat.data;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

@Data
public class CustomChatDTO {
    private String userId;
    private String message;
    private String chatId;
    private String role;
    private String modelName;
    private String conversationId;


    public String getConversationId() {
        if (StringUtils.isBlank(conversationId)) {
            this.conversationId = UUID.randomUUID().toString();
        }
        return conversationId;
    }
}