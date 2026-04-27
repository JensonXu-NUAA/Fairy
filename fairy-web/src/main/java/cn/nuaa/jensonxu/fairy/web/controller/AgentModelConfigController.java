package cn.nuaa.jensonxu.fairy.web.controller;

import cn.nuaa.jensonxu.fairy.common.data.file.response.CustomResponse;
import cn.nuaa.jensonxu.fairy.common.data.llm.agent.request.AgentModelConfigFormDTO;
import cn.nuaa.jensonxu.fairy.common.data.llm.agent.response.AgentModelConfigVO;
import cn.nuaa.jensonxu.fairy.service.agent.AgentModelConfigService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/agent/model/config")
@RequiredArgsConstructor
public class AgentModelConfigController {

    private final AgentModelConfigService agentModelConfigService;

    @PostMapping
    public CustomResponse<String> addConfig(@Valid @RequestBody AgentModelConfigFormDTO agentModelConfigFormDTO) {
        try {
            agentModelConfigService.addConfig(agentModelConfigFormDTO);
            return CustomResponse.success("模型配置新增成功");
        } catch (IllegalArgumentException e) {
            return CustomResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("[agent] 新增配置失败, userId: {}, modelName: {}", agentModelConfigFormDTO.getUserId(), agentModelConfigFormDTO.getModelName(), e);
            return CustomResponse.error("新增失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public CustomResponse<String> updateConfig(@PathVariable Long id, @Valid @RequestBody AgentModelConfigFormDTO agentModelConfigFormDTO) {
        try {
            agentModelConfigService.updateConfig(id, agentModelConfigFormDTO);
            return CustomResponse.success("模型配置更新成功");
        } catch (IllegalArgumentException e) {
            return CustomResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("[agent] 更新配置失败, id: {}", id, e);
            return CustomResponse.error("更新失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public CustomResponse<String> deleteConfig(@PathVariable Long id) {
        try {
            agentModelConfigService.deleteConfig(id);
            return CustomResponse.success("模型配置已删除");
        } catch (IllegalArgumentException e) {
            return CustomResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("[agent] 删除配置失败, id: {}", id, e);
            return CustomResponse.error("删除失败：" + e.getMessage());
        }
    }

    @GetMapping("/models")
    public CustomResponse<List<String>> listModels(@RequestParam("userId") String userId) {
        try {
            return CustomResponse.success(agentModelConfigService.listModelNames(userId));
        } catch (Exception e) {
            log.error("[agent] 查询模型名称列表失败, userId: {}", userId, e);
            return CustomResponse.error("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/config")
    public CustomResponse<List<AgentModelConfigVO>> listConfigs(@RequestParam("userId") String userId) {
        try {
            return CustomResponse.success(agentModelConfigService.listConfigs(userId));
        } catch (Exception e) {
            log.error("[agent] 查询配置列表失败, userId: {}", userId, e);
            return CustomResponse.error("查询失败：" + e.getMessage());
        }
    }
}