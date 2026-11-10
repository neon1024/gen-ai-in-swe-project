package com.ubb.genai.eventlifecycle.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ubb.genai.eventlifecycle.shared.model.EventCategory;
import com.ubb.genai.eventlifecycle.shared.port.CurrentUserProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DiscoveryController.class)
class DiscoveryControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EventSearchService service;

    @MockitoBean
    private CurrentUserProvider currentUser;

    private final EventSearchResult empty = new EventSearchResult(List.of(), 0, 20, 0, List.of());

    @Test
    @DisplayName("US-25 query parameters bind to the criteria")
    void us25_queryParametersBindToCriteria() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(currentUser.currentUserId()).thenReturn(Optional.of(userId));
        when(service.search(any(), any())).thenReturn(empty);

        mvc.perform(get("/api/discovery/events")
                        .param("q", "  chess ")
                        .param("from", "2031-01-01T10:00:00Z")
                        .param("to", "2031-01-02T10:00:00Z")
                        .param("organizationId", orgId.toString())
                        .param("category", "SOCIAL")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));

        ArgumentCaptor<EventSearchCriteria> captor = ArgumentCaptor.forClass(EventSearchCriteria.class);
        verify(service).search(captor.capture(), any());
        assertThat(captor.getValue())
                .isEqualTo(new EventSearchCriteria(
                        "chess",
                        Instant.parse("2031-01-01T10:00:00Z"),
                        Instant.parse("2031-01-02T10:00:00Z"),
                        orgId,
                        EventCategory.SOCIAL,
                        2,
                        5));
        verify(service).search(captor.getValue(), Optional.of(userId));
    }

    @Test
    @DisplayName("US-25 an unknown category returns 400")
    void us25_unknownCategoryReturns400() throws Exception {
        mvc.perform(get("/api/discovery/events").param("category", "PARTY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("US-25 invalid criteria return 400")
    void us25_invalidCriteriaReturn400() throws Exception {
        mvc.perform(get("/api/discovery/events")
                        .param("from", "2031-01-02T00:00:00Z")
                        .param("to", "2031-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(get("/api/discovery/events").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("US-24 a guest request without a header is allowed")
    void us24_guestRequestWithoutHeaderIsAllowed() throws Exception {
        when(currentUser.currentUserId()).thenReturn(Optional.empty());
        when(service.search(any(), any())).thenReturn(empty);

        mvc.perform(get("/api/discovery/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20));

        ArgumentCaptor<EventSearchCriteria> captor = ArgumentCaptor.forClass(EventSearchCriteria.class);
        verify(service).search(captor.capture(), any());
        assertThat(captor.getValue().page()).isZero();
        assertThat(captor.getValue().size()).isEqualTo(20);
    }
}
