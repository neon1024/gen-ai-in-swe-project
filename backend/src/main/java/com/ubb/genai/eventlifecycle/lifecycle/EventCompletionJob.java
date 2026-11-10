package com.ubb.genai.eventlifecycle.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically triggers {@link EventCompletionService}. Disable with {@code eventlifecycle.completion.enabled=false}. */
@Component
@ConditionalOnProperty(name = "eventlifecycle.completion.enabled", matchIfMissing = true)
public class EventCompletionJob {

    private static final Logger log = LoggerFactory.getLogger(EventCompletionJob.class);

    private final EventCompletionService service;

    public EventCompletionJob(EventCompletionService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${eventlifecycle.completion.interval:PT1M}")
    public void run() {
        try {
            int completed = service.completeEndedEvents();
            if (completed > 0) {
                log.info("Completed {} ended event(s)", completed);
            }
        } catch (RuntimeException e) {
            log.warn("Event completion run failed and will be retried: {}", e.toString());
        }
    }
}
