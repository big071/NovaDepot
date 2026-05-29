package com.novadepot.backend.modules.customerservice;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerServiceFacadeTest {
    @Test
    void delegatesConversationAndTicketCallsWithoutChangingResponseShape() {
        CustomerConversationService conversationService = mock(CustomerConversationService.class);
        CustomerTicketService ticketService = mock(CustomerTicketService.class);
        CustomerAiSuggestionService aiSuggestionService = mock(CustomerAiSuggestionService.class);
        CustomerFaqService faqService = mock(CustomerFaqService.class);
        CustomerServiceService service = new CustomerServiceService(conversationService, ticketService, aiSuggestionService, faqService);

        when(conversationService.sessions()).thenReturn(List.of(Map.of("sessionNo", "CS-1")));
        when(ticketService.tickets(null, 1, 10)).thenReturn(Map.of("list", List.of(), "total", 0L, "pageNo", 1, "pageSize", 10));

        assertThat(service.sessions()).containsExactly(Map.of("sessionNo", "CS-1"));
        assertThat(service.tickets(null, 1, 10)).containsKeys("list", "total", "pageNo", "pageSize");
        verify(conversationService).sessions();
        verify(ticketService).tickets(null, 1, 10);
    }
}
