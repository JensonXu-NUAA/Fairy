package cn.nuaa.jensonxu.fairy.integration.agent.skill;

import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 混合技能注册表
 * 聚合 NativeSkillRegistry（内置）和 UserSkillRegistry（用户自定义）
 * 对 SkillsAgentHook 统一暴露，生命周期为会话级实例
 * 同名冲突策略：用户自定义技能优先于内置技能
 */
@Slf4j
public class MixedSkillRegistry implements SkillRegistry {

    private final NativeSkillRegistry nativeRegistry;
    private final UserSkillRegistry userRegistry;

    public MixedSkillRegistry(NativeSkillRegistry nativeRegistry, UserSkillRegistry userRegistry) {
        this.nativeRegistry = nativeRegistry;
        this.userRegistry = userRegistry;
        log.info("[skill] MixedSkillRegistry 初始化完成，内置技能: {} 个，用户技能: {} 个", nativeRegistry.size(), userRegistry.size());
    }

    /**
     * 合并两个注册表的技能列表
     * 用 LinkedHashMap 去重：先写入内置，再写入用户自定义（用户自定义覆盖同名内置）
     */
    @Override
    public List<SkillMetadata> listAll() {
        Map<String, SkillMetadata> merged = new LinkedHashMap<>();
        nativeRegistry.listAll().forEach(s -> merged.put(s.getName(), s));
        userRegistry.listAll().forEach(s -> merged.put(s.getName(), s));
        return new ArrayList<>(merged.values());
    }

    /**
     * 按名称查找技能：用户自定义优先，未找到则回退到内置
     */
    @Override
    public Optional<SkillMetadata> get(String skillName) {
        Optional<SkillMetadata> userSkill = userRegistry.get(skillName);
        if (userSkill.isPresent()) {
            return userSkill;
        }
        return nativeRegistry.get(skillName);
    }

    /**
     * 任一注册表包含该技能即返回 true
     */
    @Override
    public boolean contains(String skillName) {
        return userRegistry.contains(skillName) || nativeRegistry.contains(skillName);
    }

    @Override
    public int size() {
        // 使用 listAll 的去重结果计算总数，避免同名技能被重复计数
        return listAll().size();
    }

    /**
     * 级联重载两个子注册表
     */
    @Override
    public void reload() {
        nativeRegistry.reload();
        userRegistry.reload();
        log.info("[skill] MixedSkillRegistry 重新加载完成，内置: {} 个，用户: {} 个", nativeRegistry.size(), userRegistry.size());
    }

    /**
     * 将 read_skill 请求路由到实际持有该技能的子注册表
     * 同名时优先路由到用户自定义注册表（与 get 策略一致）
     */
    @Override
    public String readSkillContent(String skillName) throws IOException {
        if (userRegistry.contains(skillName)) {
            log.debug("[skill] readSkillContent 路由到 UserSkillRegistry，skillName: {}", skillName);
            return userRegistry.readSkillContent(skillName);
        }
        if (nativeRegistry.contains(skillName)) {
            log.debug("[skill] readSkillContent 路由到 NativeSkillRegistry，skillName: {}", skillName);
            return nativeRegistry.readSkillContent(skillName);
        }
        throw new IOException("[skill] 技能在任何注册表中均不存在，skillName: " + skillName);
    }

    /**
     * 使用 NativeSkillRegistry 的加载指令作为统一标准
     */
    @Override
    public String getSkillLoadInstructions() {
        return nativeRegistry.getSkillLoadInstructions();
    }

    @Override
    public String getRegistryType() {
        return "mixed";
    }

    /**
     * 使用 NativeSkillRegistry 的 System Prompt 模板
     */
    @Override
    public SystemPromptTemplate getSystemPromptTemplate() {
        return nativeRegistry.getSystemPromptTemplate();
    }
}