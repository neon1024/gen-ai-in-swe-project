package com.ubb.genai.events.authoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubb.genai.events.authoring.application.EventAuthoringService;
import com.ubb.genai.events.authoring.domain.AuthoringEvent;
import com.ubb.genai.events.authoring.persistence.AuthoringEventRepository;
import com.ubb.genai.events.authoring.ports.AuthoringIdentityPort;
import com.ubb.genai.events.authoring.ports.EventTeamAccessPort;
import com.ubb.genai.events.authoring.ports.OrganizationAccessPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

class AuthoringAdapterConfigurationTests {
  @Test
  void ordinaryAdaptersReplaceDefaultsWhenFeatureConfigRegisteredFirst() {
    assertOrdinaryAdapters(
        new ApplicationContextRunner()
            .withUserConfiguration(AuthoringConfiguration.class, OrdinaryAdapters.class));
  }

  @Test
  void ordinaryAdaptersReplaceDefaultsWhenFeatureConfigRegisteredLast() {
    assertOrdinaryAdapters(
        new ApplicationContextRunner()
            .withUserConfiguration(OrdinaryAdapters.class, AuthoringConfiguration.class));
  }

  private void assertOrdinaryAdapters(ApplicationContextRunner runner) {
    runner.run(
        context -> {
          assertThat(context)
              .hasNotFailed()
              .hasSingleBean(AuthoringIdentityPort.class)
              .hasSingleBean(OrganizationAccessPort.class)
              .hasSingleBean(EventTeamAccessPort.class)
              .hasSingleBean(Clock.class)
              .hasSingleBean(EventAuthoringService.class);
          var result =
              context
                  .getBean(EventAuthoringService.class)
                  .createDraft(
                      new EventAuthoringService.CreateEventCommand(
                          "T",
                          "D",
                          com.ubb.genai.events.authoring.domain.EventCategory.OTHER,
                          "org"));
          assertThat(result.createdAt()).isEqualTo(Instant.parse("2026-11-10T10:00:00Z"));
        });
  }

  @Test
  void ordinaryServiceCanReplaceDefaultService() {
    new ApplicationContextRunner()
        .withUserConfiguration(
            AuthoringConfiguration.class, OrdinaryAdapters.class, OrdinaryService.class)
        .run(
            context ->
                assertThat(context).hasNotFailed().hasSingleBean(EventAuthoringService.class));
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class OrdinaryAdapters {
    @Bean
    AuthoringIdentityPort ordinaryIdentity() {
      return () -> new AuthoringIdentityPort.Actor("real", true);
    }

    @Bean
    OrganizationAccessPort ordinaryOrganizations() {
      return (s, o) -> OrganizationAccessPort.Role.ORGANIZER;
    }

    @Bean
    EventTeamAccessPort ordinaryTeam() {
      return (s, e) -> EventTeamAccessPort.Role.NONE;
    }

    @Bean
    Clock ordinaryClock() {
      return Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), ZoneOffset.UTC);
    }

    @Bean
    AuthoringEventRepository ordinaryRepository() {
      return new AuthoringEventRepository() {
        public AuthoringEvent save(AuthoringEvent event) {
          return event;
        }

        public Optional<AuthoringEvent> findById(UUID id) {
          return Optional.empty();
        }
      };
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class OrdinaryService {
    @Bean
    EventAuthoringService ordinaryService(
        AuthoringIdentityPort identity,
        OrganizationAccessPort org,
        EventTeamAccessPort team,
        AuthoringEventRepository repository,
        Clock clock) {
      return new EventAuthoringService(identity, org, team, repository, clock);
    }
  }
}
