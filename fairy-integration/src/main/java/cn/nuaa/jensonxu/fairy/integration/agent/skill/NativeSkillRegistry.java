package cn.nuaa.jensonxu.fairy.integration.agent.skill;

import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.classpath.ClasspathSkillRegistry;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * 内置技能注册表
 * 加载 fairy-integration classpath 下 skills/ 目录中的预置技能
 * 对所有用户可见
 */

@Slf4j
@Component
public class NativeSkillRegistry implements SkillRegistry {

    private static final String CLASSPATH_SKILLS_PATH = "skills";

    private final ClasspathSkillRegistry delegate;

    public NativeSkillRegistry() {
        this.delegate = ClasspathSkillRegistry.builder()
                .classpathPath(CLASSPATH_SKILLS_PATH)
                .build();
        log.info("[skill] NativeSkillRegistry 初始化完成，已加载 {} 个内置技能", delegate.size());
    }

    @Override
    public Optional<SkillMetadata> get(String skillName) {
        return delegate.get(skillName);
    }

    @Override
    public List<SkillMetadata> listAll() {
        return delegate.listAll();
    }

    @Override
    public boolean contains(String skillName) {
        return delegate.contains(skillName);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public void reload() {
        delegate.reload();
        log.info("[skill] NativeSkillRegistry 重新加载，当前技能数: {}", delegate.size());
    }

    @Override
    public String readSkillContent(String skillName) throws IOException {
        return delegate.readSkillContent(skillName);
    }

    @Override
    public String getSkillLoadInstructions() {
        return delegate.getSkillLoadInstructions();
    }

    @Override
    public String getRegistryType() {
        return "native";
    }

    @Override
    public SystemPromptTemplate getSystemPromptTemplate() {
        return delegate.getSystemPromptTemplate();
    }
}
