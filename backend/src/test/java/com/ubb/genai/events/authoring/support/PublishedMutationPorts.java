package com.ubb.genai.events.authoring.support;

import com.ubb.genai.events.authoring.application.AuthoringException;
import com.ubb.genai.events.authoring.ports.*;
import java.time.*;
import java.util.function.Function;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class PublishedMutationPorts {
  public static class Probe extends Clock implements RegistrationAuthoringPort {
    public String subject = "owner";
    public OrganizationAccessPort.Role organization = OrganizationAccessPort.Role.ORGANIZER;
    public EventTeamAccessPort.Role team = EventTeamAccessPort.Role.NONE;
    public long confirmed;
    public boolean history,
        unavailable,
        failAfter,
        failPromotion,
        guardedTransaction,
        promotedTransaction;
    public Instant now = Instant.parse("2026-11-10T10:00:00Z");
    public Instant lockedNow, promotionNow, promotionStart;
    public int calls, promotions, promotedCapacity;
    public GuardRequest requested;

    public void reset() {
      subject = "owner";
      organization = OrganizationAccessPort.Role.ORGANIZER;
      team = EventTeamAccessPort.Role.NONE;
      confirmed = 0;
      history = false;
      unavailable = false;
      failAfter = false;
      failPromotion = false;
      now = Instant.parse("2026-11-10T10:00:00Z");
      lockedNow = null;
      promotionNow = null;
      promotionStart = null;
      calls = 0;
      promotions = 0;
      promotedCapacity = 0;
      requested = null;
      guardedTransaction = false;
      promotedTransaction = false;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return Clock.fixed(now, zone);
    }

    @Override
    public Instant instant() {
      return now;
    }

    public <T> T withLockedActivity(
        GuardRequest request, Function<RegistrationContext, T> mutation) {
      calls++;
      requested = request;
      guardedTransaction =
          org.springframework.transaction.support.TransactionSynchronizationManager
              .isActualTransactionActive();
      if (unavailable) throw AuthoringException.unavailable();
      if (lockedNow != null) now = lockedNow;
      T result =
          mutation.apply(
              new RegistrationContext() {
                public long confirmedCount() {
                  return confirmed;
                }

                public boolean hasAnySignup() {
                  return history;
                }

                public void promoteEligibleWaitlist(int capacity, Instant start, Instant at) {
                  promotedTransaction =
                      org.springframework.transaction.support.TransactionSynchronizationManager
                          .isActualTransactionActive();
                  promotions++;
                  promotedCapacity = capacity;
                  promotionStart = start;
                  promotionNow = at;
                  if (failPromotion) throw AuthoringException.unavailable();
                }
              });
      if (failAfter) throw AuthoringException.unavailable();
      return result;
    }
  }

  @Bean
  public Probe probe() {
    return new Probe();
  }

  @Bean
  AuthoringIdentityPort identity(Probe p) {
    return () -> new AuthoringIdentityPort.Actor(p.subject, true);
  }

  @Bean
  OrganizationAccessPort organizations(Probe p) {
    return (s, o) -> p.organization;
  }

  @Bean
  EventTeamAccessPort team(Probe p) {
    return (s, e) -> p.team;
  }
}
