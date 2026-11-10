package com.ubb.genai.eventlifecycle.search;

import com.ubb.genai.eventlifecycle.shared.model.EventCategory;
import com.ubb.genai.eventlifecycle.shared.port.CurrentUserProvider;
import com.ubb.genai.eventlifecycle.shared.web.LifecycleProblem;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    private final EventSearchService service;
    private final CurrentUserProvider currentUser;

    public DiscoveryController(EventSearchService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/events")
    public EventSearchResult search(
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) EventCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + EventSearchCriteria.DEFAULT_SIZE) int size) {
        EventSearchCriteria criteria;
        try {
            criteria = new EventSearchCriteria(keyword, from, to, organizationId, category, page, size);
        } catch (IllegalArgumentException e) {
            throw LifecycleProblem.badRequest("VALIDATION_FAILED", e.getMessage());
        }
        return service.search(criteria, currentUser.currentUserId());
    }
}
