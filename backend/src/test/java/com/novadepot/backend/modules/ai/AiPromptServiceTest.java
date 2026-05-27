package com.novadepot.backend.modules.ai;

import com.novadepot.backend.common.context.RequestContext;
import com.novadepot.backend.model.entity.AIPromptTemplateEntity;
import com.novadepot.backend.repository.AIPromptTemplateMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiPromptServiceTest {

    private final AIPromptTemplateMapper promptTemplateMapper = mock(AIPromptTemplateMapper.class);
    private final AiPromptService promptService = new AiPromptService(promptTemplateMapper);

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void normalizeScene_defaultsToEnterpriseAndLowercases() {
        assertThat(promptService.normalizeScene(null)).isEqualTo("enterprise");
        assertThat(promptService.normalizeScene(" WAREHOUSE ")).isEqualTo("warehouse");
    }

    @Test
    void knowledgeScene_mapsSopToCustomerService() {
        assertThat(promptService.knowledgeScene("sop")).isEqualTo("customer-service");
        assertThat(promptService.knowledgeScene("enterprise")).isEqualTo("enterprise");
    }

    @Test
    void renderPrompt_usesEnabledDatabaseTemplateWhenPresent() {
        RequestContext.setTenantId(1L);
        AIPromptTemplateEntity template = new AIPromptTemplateEntity();
        template.setTemplateContent("模板问题：{{question}}");
        when(promptTemplateMapper.selectOne(any())).thenReturn(template);

        assertThat(promptService.renderPrompt("enterprise", "库存怎么样"))
                .isEqualTo("模板问题：库存怎么样");
    }

    @Test
    void renderPrompt_appendsQuestionWhenTemplateHasNoPlaceholder() {
        RequestContext.setTenantId(1L);
        AIPromptTemplateEntity template = new AIPromptTemplateEntity();
        template.setTemplateContent("固定模板");
        when(promptTemplateMapper.selectOne(any())).thenReturn(template);

        assertThat(promptService.renderPrompt("enterprise", "库存怎么样"))
                .isEqualTo("固定模板\n用户问题：库存怎么样");
    }

    @Test
    void renderPrompt_usesCurrentEnterpriseDefaultPromptWhenNoTemplate() {
        RequestContext.setTenantId(1L);
        when(promptTemplateMapper.selectOne(any())).thenReturn(null);

        String prompt = promptService.renderPrompt("enterprise", "库存怎么样");

        assertThat(prompt).contains("你是 NovaDepot 企业业务助手，面向仓库主管、客服和管理员。");
        assertThat(prompt).contains("输出结构：当前结论、主要风险、建议动作、数据依据、下一步可执行操作。");
        assertThat(prompt).contains("问题：库存怎么样");
    }
}
