package cn.nuaa.jensonxu.fairy.integration.service.tools.service;

/**
 * 技能绑定工具标记接口
 * 实现此接口的工具不注册到全局工具列表，而是在对应 Skill 被 read_skill 调用后才对模型可见
 * 与 McpToolService 互斥，不可同时实现
 */
public interface SkillToolService {

    /**
     * 返回此工具所绑定的 Skill 名称，需与 SKILL.md 中的 name 字段完全一致
     */
    String getSkillName();
}