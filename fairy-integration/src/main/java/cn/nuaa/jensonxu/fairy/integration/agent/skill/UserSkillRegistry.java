package cn.nuaa.jensonxu.fairy.integration.agent.skill;

import cn.nuaa.jensonxu.fairy.common.repository.mysql.AgentSkillRepository;
import cn.nuaa.jensonxu.fairy.common.repository.mysql.data.AgentSkillDO;
import cn.nuaa.jensonxu.fairy.common.repository.minio.MinioProperties;

import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.registry.AbstractSkillRegistry;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户自定义技能注册表
 * 元数据来源：MySQL（agent_user_skill）
 * 技能内容来源：MinIO（SKILL.md 文件）
 * 构造时绑定 userId，只加载当前用户的 skills
 */
@Slf4j
public class UserSkillRegistry extends AbstractSkillRegistry {

    private final String userId;
    private final AgentSkillRepository skillRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /** SKILL.md 内容缓存，避免同一会话内重复拉取 MinIO */
    private final Map<String, String> contentCache = new ConcurrentHashMap<>();

    public UserSkillRegistry(String userId, AgentSkillRepository skillRepository, MinioClient minioClient, MinioProperties minioProperties) {
        this.userId = userId;
        this.skillRepository = skillRepository;
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
        reload();
        log.info("[skill] UserSkillRegistry 初始化完成，userId: {}，已加载 {} 个用户技能", userId, size());
    }

    /**
     * 从 MySQL 加载当前用户已启用的技能元数据，填充 AbstractSkillRegistry.skills
     */
    @Override
    protected void loadSkillsToRegistry() {
        skills.clear();
        List<AgentSkillDO> records = skillRepository.findEnabledByUserId(userId);
        for (AgentSkillDO record : records) {
            SkillMetadata metadata = SkillMetadata.builder()
                    .name(record.getSkillName())
                    .description(record.getDescription())
                    .skillPath(record.getMinioPath())
                    .source("user:" + userId)
                    .build();
            skills.put(record.getSkillName(), metadata);
        }
        log.debug("[skill] loadSkillsToRegistry 完成，userId: {}，技能数: {}", userId, skills.size());
    }

    /**
     * 重新加载时同时清空内容缓存，确保拉取到最新 SKILL.md
     */
    @Override
    public synchronized void reload() {
        contentCache.clear();
        super.reload();
    }

    /**
     * 从 MinIO 拉取指定技能的 SKILL.md 内容
     * 命中缓存时直接返回，避免重复 I/O
     */
    @Override
    public String readSkillContent(String skillName) throws IOException {
        if (contentCache.containsKey(skillName)) {
            log.debug("[skill] 命中内容缓存，skillName: {}", skillName);
            return contentCache.get(skillName);
        }

        SkillMetadata metadata = skills.get(skillName);
        if (metadata == null) {
            throw new IOException("[skill] 技能不存在或未启用，skillName: " + skillName);
        }

        String content = fetchFromMinio(metadata.getSkillPath());
        contentCache.put(skillName, content);
        log.info("[skill] SKILL.md 已从 MinIO 加载，skillName: {}, path: {}", skillName, metadata.getSkillPath());
        return content;
    }

    /**
     * 从 MinIO 下载指定路径的对象并转换为字符串
     */
    private String fetchFromMinio(String objectPath) throws IOException {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(objectPath)
                        .build())) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IOException("[skill] 从 MinIO 加载 SKILL.md 失败，path: " + objectPath, e);
        }
    }

    @Override
    public String getSkillLoadInstructions() {
        return "To load a user-defined skill, call the read_skill function with the skill name.";
    }

    @Override
    public String getRegistryType() {
        return "user";
    }

    /**
     * 返回 null，由 MixedSkillRegistry 统一使用 NativeSkillRegistry 的模板
     */
    @Override
    public SystemPromptTemplate getSystemPromptTemplate() {
        return null;
    }
}