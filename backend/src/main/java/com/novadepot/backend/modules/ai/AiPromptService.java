package com.novadepot.backend.modules.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.AIPromptTemplateEntity;
import com.novadepot.backend.repository.AIPromptTemplateMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiPromptService {
    private final AIPromptTemplateMapper promptTemplateMapper;

    public AiPromptService(AIPromptTemplateMapper promptTemplateMapper) {
        this.promptTemplateMapper = promptTemplateMapper;
    }

    public String renderPrompt(String scene, String userMessage) {
        AIPromptTemplateEntity template = promptTemplateMapper.selectOne(new LambdaQueryWrapper<AIPromptTemplateEntity>()
                .eq(AIPromptTemplateEntity::getTenantId, RequestContext.tenantId())
                .eq(AIPromptTemplateEntity::getScene, scene)
                .eq(AIPromptTemplateEntity::getEnabled, 1)
                .orderByDesc(AIPromptTemplateEntity::getVersionNo)
                .last("limit 1"));

        String content;
        if (template != null && StringUtils.hasText(template.getTemplateContent())) {
            content = template.getTemplateContent();
        } else {
            content = defaultPromptByScene(scene);
        }

        if (content.contains("{{question}}")) {
            return content.replace("{{question}}", userMessage);
        }
        return content + "\n用户问题：" + userMessage;
    }

    public String defaultPromptByScene(String scene) {
        return switch (scene) {
            case "warehouse" -> """
                    你是 NovaDepot 仓库业务助手。请只基于库存、入库、出库、盘点或工具查询事实回答。
                    输出结构：当前结论、主要风险、建议动作、数据依据、下一步可执行操作。
                    不要暴露内部工具名，不要编造库存、单号或状态。
                    问题：{{question}}""";
            case "sop" -> """
                    你是 NovaDepot 客服 SOP 助手。请面向客服人员输出可执行步骤、风险点和复核动作。
                    输出结构：当前结论、主要风险、建议动作、数据依据、下一步可执行操作。
                    没有查询到数据时必须说明“未查询到相关数据”。
                    问题：{{question}}""";
            case "enterprise" -> """
                    你是 NovaDepot 企业业务助手，面向仓库主管、客服和管理员。
                    输出结构：当前结论、主要风险、建议动作、数据依据、下一步可执行操作。
                    有工具结果时必须直接融合查询结果；没有工具结果时不要编造库存、金额、单号、状态。
                    不承诺已经执行审核、发货、付款、改库存等写操作。
                    问题：{{question}}""";
            default -> """
                    你是 NovaDepot AI 助手。请输出中文、结构化、可执行的业务建议。
                    输出结构：当前结论、主要风险、建议动作、数据依据、下一步可执行操作。
                    问题：{{question}}""";
        };
    }

    public String normalizeScene(String scene) {
        if (!StringUtils.hasText(scene)) {
            return "enterprise";
        }
        return scene.trim().toLowerCase();
    }

    public String knowledgeScene(String scene) {
        return "sop".equalsIgnoreCase(scene) ? "customer-service" : scene;
    }
}
