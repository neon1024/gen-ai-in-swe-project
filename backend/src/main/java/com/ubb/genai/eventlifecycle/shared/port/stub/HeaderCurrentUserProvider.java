package com.ubb.genai.eventlifecycle.shared.port.stub;

import com.ubb.genai.eventlifecycle.shared.port.CurrentUserProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Stub: replace with the real implementation from neon1024 at reconciliation.
 * Reads the caller from the {@code X-User-Id} header.
 */
@Component
public class HeaderCurrentUserProvider implements CurrentUserProvider {

    public static final String HEADER = "X-User-Id";

    @Override
    public Optional<UUID> currentUserId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servlet)) {
            return Optional.empty();
        }
        String value = servlet.getRequest().getHeader(HEADER);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
