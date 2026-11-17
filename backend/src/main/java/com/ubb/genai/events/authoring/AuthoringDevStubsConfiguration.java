package com.ubb.genai.events.authoring;

import com.ubb.genai.events.authoring.ports.AuthoringIdentityPort;
import com.ubb.genai.events.authoring.ports.EventTeamAccessPort;
import com.ubb.genai.events.authoring.ports.OrganizationAccessPort;
import com.ubb.genai.events.authoring.ports.RegistrationAuthoringPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Development stubs, like the lifecycle share's {@code X-User-Id} stub: the caller is whoever the
 * {@code X-User-Id} header names, with every organization role and no signups. Off unless
 * {@code authoring.dev-stubs.enabled=true}; replace with the real identity, organization, team and
 * registration adapters at reconciliation.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "authoring.dev-stubs.enabled", havingValue = "true")
public class AuthoringDevStubsConfiguration {
  static final String HEADER = "X-User-Id";

  @Bean
  AuthoringChainCustomizer devHeaderAuthentication() {
    return http -> http.addFilterBefore(new HeaderAuthenticationFilter(), AuthorizationFilter.class);
  }

  @Bean
  AuthoringIdentityPort devIdentity() {
    return () ->
        new AuthoringIdentityPort.Actor(
            SecurityContextHolder.getContext().getAuthentication().getName(), true);
  }

  @Bean
  OrganizationAccessPort devOrganizations() {
    return (subject, organizationRef) -> OrganizationAccessPort.Role.ORG_ADMIN;
  }

  @Bean
  EventTeamAccessPort devTeam() {
    return (subject, eventId) -> EventTeamAccessPort.Role.NONE;
  }

  @Bean
  RegistrationAuthoringPort devRegistration() {
    return new RegistrationAuthoringPort() {
      public <T> T withLockedActivity(GuardRequest request, Function<RegistrationContext, T> mutation) {
        return mutation.apply(
            new RegistrationContext() {
              public long confirmedCount() { return 0; }
              public boolean hasAnySignup() { return false; }
              public void promoteEligibleWaitlist(int newCapacity, Instant startsAt, Instant now) {}
            });
      }
    };
  }

  static final class HeaderAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
      String user = request.getHeader(HEADER);
      if (user != null && user.trim().matches("[A-Za-z0-9-]{1,64}")) {
        SecurityContextHolder.getContext()
            .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user.trim(), null, List.of()));
      }
      chain.doFilter(request, response);
    }
  }
}
