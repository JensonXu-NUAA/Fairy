package cn.nuaa.jensonxu.fairy.integration.agent;

import cn.nuaa.jensonxu.fairy.common.data.llm.ModelConfig;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.AgentSessionMetadataRepository;

import cn.nuaa.jensonxu.fairy.integration.agent.model.manager.NativeModelManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentSessionTitleGenerator {

    private final NativeModelManager nativeModelManager;
    private final AgentProperties agentProperties;
    private final AgentSessionMetadataRepository metadataRepository;

    private static final String SYSTEM_PROMPT = """
            你是一个对话标题生成助手。根据用户的第一条消息，生成一个简洁的会话标题。
            规则：
            1. 标题长度不超过 20 个字
            2. 直接输出标题文本，不加引号、不加任何解释
            3. 语言与用户消息保持一致
            """;

    /**
     * 异步生成会话标题并回填到数据库
     *
     * @param sessionId   会话ID
     * @param firstMessage 用户首条消息内容
     */
    @Async
    public void generateAndSave(String sessionId, String firstMessage) {
        log.info("[title-gen] 开始生成会话标题, sessionId: {}", sessionId);
        try {
            String modelName = agentProperties.getMemory().getLongTerm().getModelName();
            ModelConfig modelConfig = nativeModelManager.getSummaryModelConfig(modelName);

            OpenAiApi openAiApi = OpenAiApi.builder()
                    .apiKey(modelConfig.getApiKey())
                    .baseUrl(modelConfig.getBaseUrl())
                    .build();

            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(openAiApi)
                    .defaultOptions(OpenAiChatOptions.builder()
                            .model(modelConfig.getModelName())
                            .build())
                    .build();

            String title = ChatClient.builder(chatModel).build()
                    .prompt()
                    .system(SYSTEM_PROMPT)
                    .user(firstMessage)
                    .call()
                    .content();

            if (StringUtils.isNotBlank(title)) {
                // 超长截断兜底
                title = title.trim();
                if (title.length() > 200) {
                    title = title.substring(0, 200);
                }
                metadataRepository.updateTitle(sessionId, title);
                log.info("[title] 标题生成成功, sessionId: {}, title: {}", sessionId, title);
            }
        } catch (Exception e) {
            log.error("[title] 标题生成失败, sessionId: {}", sessionId, e);  // 失败不影响主流程，title 保持 null，前端可展示兜底文案
        }
    }
}
