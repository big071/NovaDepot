package com.novadepot.backend.smoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MainlineSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String adminToken;
    private static String operatorToken;
    private static Long createdInboundId;
    private static Long createdOutboundId;
    private static String latestAiConversationNo;
    private static Long chosenCsSessionId;
    private static String createdTicketNo;

    @BeforeAll
    static void beforeAll() {
        adminToken = null;
    }

    @Test
    @Order(1)
    void login_shouldReturnToken() throws Exception {
        adminToken = login("admin", "admin123");
        operatorToken = login("warehouse01", "pass123");
        assertThat(adminToken).isNotBlank();
        assertThat(operatorToken).isNotBlank();
    }

    @Test
    @Order(2)
    void dashboard_shouldLoad() throws Exception {
        authedGet("/api/v1/reports/dashboard")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    @Order(3)
    void masterAndInventoryLists_shouldLoad() throws Exception {
        assertListNotEmpty("/api/v1/products");
        assertListNotEmpty("/api/v1/warehouses");
        assertListNotEmpty("/api/v1/locations");
        assertListNotEmpty("/api/v1/inventory");
    }

    @Test
    @Order(4)
    void inbound_shouldCreateApproveAndPost() throws Exception {
        String payload = """
                {
                  "warehouseId": 11001,
                  "supplierId": 21001,
                  "items": [
                    {"productId": 13001, "locationId": 12001, "qty": 3}
                  ]
                }
                """;

        MvcResult createResult = mockMvc.perform(
                        post("/api/v1/inbound-orders")
                                .header("Authorization", bearer(operatorToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();

        createdInboundId = parseDataNode(createResult).path("id").asLong();
        assertThat(createdInboundId).isPositive();

        authedPost("/api/v1/inbound-orders/" + createdInboundId + "/actions/submit", "{}", operatorToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        authedPost("/api/v1/inbound-orders/" + createdInboundId + "/actions/approve", "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        authedPost("/api/v1/inbound-orders/" + createdInboundId + "/actions/post", "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    @Order(5)
    void outbound_shouldCreateApproveAndShip() throws Exception {
        String payload = """
                {
                  "warehouseId": 11001,
                  "customerId": 22001,
                  "items": [
                    {"productId": 13002, "locationId": 12002, "qty": 2}
                  ]
                }
                """;

        MvcResult createResult = mockMvc.perform(
                        post("/api/v1/outbound-orders")
                                .header("Authorization", bearer(operatorToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();

        createdOutboundId = parseDataNode(createResult).path("id").asLong();
        assertThat(createdOutboundId).isPositive();

        authedPost("/api/v1/outbound-orders/" + createdOutboundId + "/actions/submit", "{}", operatorToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        authedPost("/api/v1/outbound-orders/" + createdOutboundId + "/actions/approve", "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        authedPost("/api/v1/outbound-orders/" + createdOutboundId + "/actions/ship", "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    @Order(6)
    void ai_shouldListChatAndReplayHistory() throws Exception {
        MvcResult listResult = authedGet("/api/v1/ai/conversations")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();

        JsonNode conversationList = parseDataNode(listResult);
        assertThat(conversationList.isArray()).isTrue();
        assertThat(conversationList.size()).isGreaterThan(0);

        String chatPayload = """
                {
                  "scene": "enterprise",
                  "message": "请给我一个库存健康度摘要",
                  "providerHint": "rule"
                }
                """;

        MvcResult chatResult = authedPost("/api/v1/ai/chat", chatPayload)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.conversationNo").exists())
                .andReturn();

        latestAiConversationNo = parseDataNode(chatResult).path("conversationNo").asText();
        assertThat(latestAiConversationNo).isNotBlank();

        authedGet("/api/v1/ai/conversations/by-no/" + latestAiConversationNo + "/messages")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data").isArray());

        MvcResult createdConversation = authedPost("/api/v1/ai/conversations?scene=enterprise", "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();
        Long conversationId = parseDataNode(createdConversation).path("id").asLong();
        assertThat(conversationId).isPositive();

        MvcResult streamResult = mockMvc.perform(
                        post("/api/v1/ai/chat/stream?requestId=smoke-stream")
                                .header("Authorization", bearer())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "scene": "enterprise",
                                          "message": "stream smoke test",
                                          "conversationId": %d,
                                          "providerHint": "rule"
                                        }
                                        """.formatted(conversationId))
                )
                .andExpect(request().asyncStarted())
                .andReturn();

        streamResult.getAsyncResult(5000);
        authedGet("/api/v1/ai/conversations/" + conversationId + "/messages")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data").isArray());

        authedPost("/api/v1/ai/conversations/" + conversationId + "/archive", "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
    }

    @Test
    @Order(7)
    void customerService_shouldCreateAndQueryTicketAndFaq() throws Exception {
        MvcResult sessionResult = authedGet("/api/v1/customer-service/sessions")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();

        JsonNode sessions = parseDataNode(sessionResult);
        assertThat(sessions.isArray()).isTrue();
        assertThat(sessions.size()).isGreaterThan(0);
        chosenCsSessionId = sessions.get(0).path("id").asLong();
        assertThat(chosenCsSessionId).isPositive();

        authedPost("/api/v1/customer-service/sessions/" + chosenCsSessionId + "/messages", """
                {
                  "content": "测试发送客服消息",
                  "msgType": "TEXT",
                  "sendByAi": false
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        MvcResult createTicketResult = authedPost("/api/v1/customer-service/tickets", """
                {
                  "sessionId": %d,
                  "priority": "MEDIUM",
                  "content": "自动化测试工单"
                }
                """.formatted(chosenCsSessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.ticketNo").exists())
                .andReturn();

        createdTicketNo = parseDataNode(createTicketResult).path("ticketNo").asText();
        assertThat(createdTicketNo).isNotBlank();

        MvcResult ticketResult = authedGet("/api/v1/customer-service/tickets?sessionId=" + chosenCsSessionId + "&pageNo=1&pageSize=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.list").isArray())
                .andReturn();

        JsonNode ticketList = parseDataNode(ticketResult).path("list");
        boolean foundCreated = false;
        for (JsonNode item : ticketList) {
            if (createdTicketNo.equals(item.path("ticketNo").asText())) {
                foundCreated = true;
                break;
            }
        }
        assertThat(foundCreated).isTrue();

        authedGet("/api/v1/customer-service/faq")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(8)
    void permission_forViewer_shouldReturn403OnOutboundShip() throws Exception {
        String viewerToken = login("observer01", "pass123");
        mockMvc.perform(
                        post("/api/v1/outbound-orders/16003/actions/ship")
                                .header("Authorization", "Bearer " + viewerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH-0002"));
    }

    private String login(String username, String password) throws Exception {
        String body = """
                {
                  "tenantCode": "default",
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(username, password);

        MvcResult result = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andReturn();

        return parseDataNode(result).path("accessToken").asText();
    }

    private org.springframework.test.web.servlet.ResultActions authedGet(String url) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", bearer()));
    }

    private org.springframework.test.web.servlet.ResultActions authedPost(String url, String body) throws Exception {
        return authedPost(url, body, adminToken);
    }

    private org.springframework.test.web.servlet.ResultActions authedPost(String url, String body, String token) throws Exception {
        return mockMvc.perform(
                post(url)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        );
    }

    private String bearer() {
        return "Bearer " + adminToken;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private JsonNode parseDataNode(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data");
    }

    private void assertListNotEmpty(String url) throws Exception {
        MvcResult result = authedGet(url)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();
        JsonNode list = parseDataNode(result);
        assertThat(list.isArray()).isTrue();
        assertThat(list.size()).isGreaterThan(0);
    }
}
