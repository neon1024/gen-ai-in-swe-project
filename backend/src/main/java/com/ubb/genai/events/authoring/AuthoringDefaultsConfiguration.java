package com.ubb.genai.events.authoring;

import com.ubb.genai.events.authoring.application.AuthoringException;
import com.ubb.genai.events.authoring.application.EventAuthoringService;
import com.ubb.genai.events.authoring.persistence.AuthoringEventRepository;
import com.ubb.genai.events.authoring.ports.AuthoringIdentityPort;
import com.ubb.genai.events.authoring.ports.EventTeamAccessPort;
import com.ubb.genai.events.authoring.ports.OrganizationAccessPort;
import java.time.Clock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class AuthoringDefaultsConfiguration {
  @Bean
  @ConditionalOnMissingBean
  AuthoringIdentityPort authoringIdentity() {
    return () -> {
      throw AuthoringException.unavailable();
    };
  }

  @Bean
  @ConditionalOnMissingBean
  OrganizationAccessPort authoringOrganizations() {
    return (s, o) -> {
      throw AuthoringException.unavailable();
    };
  }

  @Bean
  @ConditionalOnMissingBean
  EventTeamAccessPort authoringTeam() {
    return (s, e) -> {
      throw AuthoringException.unavailable();
    };
  }

  @Bean
  @ConditionalOnMissingBean
  Clock authoringClock() {
    return Clock.systemUTC();
  }

  @Bean
  @ConditionalOnMissingBean
  EventAuthoringService eventAuthoringService(
      AuthoringIdentityPort identity,
      OrganizationAccessPort org,
      EventTeamAccessPort team,
      AuthoringEventRepository repository,
      Clock clock) {
    return new EventAuthoringService(identity, org, team, repository, clock);
  }
}
