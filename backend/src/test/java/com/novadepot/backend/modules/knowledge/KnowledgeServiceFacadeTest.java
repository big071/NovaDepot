package com.novadepot.backend.modules.knowledge;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeServiceFacadeTest {
    @Test
    void delegatesKnowledgeCallsWithoutChangingBoundaryMaps() {
        KnowledgeFaqService faqService = mock(KnowledgeFaqService.class);
        KnowledgeSopService sopService = mock(KnowledgeSopService.class);
        KnowledgeRuleService ruleService = mock(KnowledgeRuleService.class);
        CustomerKnowledgeDraftService draftService = mock(CustomerKnowledgeDraftService.class);
        KnowledgeMatchService matchService = mock(KnowledgeMatchService.class);
        KnowledgeService service = new KnowledgeService(faqService, sopService, ruleService, draftService, matchService);

        when(faqService.listFaqs(null, null, null)).thenReturn(List.of(Map.of("faqCode", "FAQ-1")));
        when(ruleService.decimalRule("LOW_STOCK", BigDecimal.TEN)).thenReturn(BigDecimal.TEN);

        assertThat(service.listFaqs(null, null, null)).containsExactly(Map.of("faqCode", "FAQ-1"));
        assertThat(service.decimalRule("LOW_STOCK", BigDecimal.TEN)).isEqualByComparingTo(BigDecimal.TEN);
        verify(faqService).listFaqs(null, null, null);
        verify(ruleService).decimalRule("LOW_STOCK", BigDecimal.TEN);
    }
}
