package cn.nuaa.jensonxu.fairy.integration.agent.memory;

import cn.nuaa.jensonxu.fairy.common.data.llm.ModelConfig;
import cn.nuaa.jensonxu.fairy.integration.agent.AgentProperties;
import cn.nuaa.jensonxu.fairy.integration.agent.model.manager.NativeModelManager;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Agent 长期记忆召回器
 * 根据当前用户消息，从索引清单中通过 LLM 侧查询选出相关的记忆条目
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentMemoryRecaller {

    private final NativeModelManager nativeModelManager;
    private final AgentProperties agentProperties;

    private static final String SYSTEM_PROMPT = """
            你是一个记忆检索助手。请根据用户当前的消息，从以下记忆索引清单中选出与本次对话最相关的记忆条目。
            
            【记忆索引清单格式】
            每行一条：memory_key | 分类 | 名称: 描述 | 更新时间
            
            【选取规则】
            1. 只选取与当前消息有实质关联的条目，不相关的一律不选
            2. 优先选取更新时间较近的条目
            3. 数量控制在 5 条以内
            4. 严格输出 JSON 数组，只包含 memory_key 字符串，不附加任何说明：
               ["key1", "key2", ...]
            5. 若无任何相关条目，输出空数组：[]
            """;

    /**
     * 从索引清单中召回与当前消息相关的记忆 key 列表
     *
     * @param userId         用户 ID（用于日志）
     * @param currentMessage 当前用户消息文本
     * @param manifest       索引清单文本（由 AgentLongTermMemory.buildMemoryManifest 生成）
     * @param validKeys      所有合法的 memoryKey 集合，用于过滤模型幻觉输出
     * @return 被选中的 memoryKey 列表，失败或无相关时返回空列表
     */
    public List<String> recall(String userId, String currentMessage, String manifest, Set<String> validKeys) {
        if (StringUtils.isBlank(currentMessage) || StringUtils.isBlank(manifest)) {
            return List.of();
        }

        String modelName = agentProperties.getMemory().getLongTerm().getModelName();
        ModelConfig modelConfig = nativeModelManager.getSummaryModelConfig(modelName);

        log.info("[recaller] 开始记忆召回, userId: {}", userId);
        try {
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

            String userContent = "【记忆索引清单】\n" + manifest + "\n\n【当前用户消息】\n" + currentMessage;
            String result = ChatClient.builder(chatModel).build()
                    .prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userContent)
                    .call()
                    .content();

            List<String> recalled = parseResult(result, validKeys);
            log.info("[recaller] 召回完成, userId: {}, 选中 {} 条", userId, recalled.size());
            return recalled;

        } catch (Exception e) {
            log.error("[recaller] 记忆召回失败, userId: {}", userId, e);
            return List.of();
        }
    }

    /**
     * 解析模型返回的 JSON 数组，过滤非法 key
     */
    private List<String> parseResult(String json, Set<String> validKeys) {
        if (StringUtils.isBlank(json)) {
            return List.of();
        }
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start < 0 || end < 0 || start >= end) {
            log.warn("[recaller] 模型返回非 JSON 格式内容: {}", json);
            return List.of();
        }
        try {
            JSONArray array = JSON.parseArray(json.substring(start, end + 1));
            List<String> result = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                String key = array.getString(i);
                if (StringUtils.isNotBlank(key) && validKeys.contains(key)) {
                    result.add(key);
                } else if (StringUtils.isNotBlank(key)) {
                    log.debug("[recaller] 过滤非法 key: {}", key);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("[recaller] JSON 解析失败, 原始内容: {}", json, e);
            return List.of();
        }
    }
}