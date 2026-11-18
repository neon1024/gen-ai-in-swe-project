package com.ubb.genai.events.authoring;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

/**
 * Session security for the SPA API: cookie CSRF token echoed as X-XSRF-TOKEN and JSON 401/403
 * bodies. Authoring routes keep their own chain (decision K-8); every other non-auth route uses
 * the baseline chain, where only event discovery and the health check are open to guests.
 */
@Configuration(proxyBeanMethods = false)
public class AuthoringSecurityConfiguration {
  @Bean
  @Order(1)
  SecurityFilterChain authoringSecurity(HttpSecurity http) throws Exception {
    return spaDefaults(http.securityMatcher("/api/event-authoring/**"))
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .build();
  }

  @Bean
  @Order(2)
  SecurityFilterChain baselineSecurity(HttpSecurity http) throws Exception {
    return spaDefaults(http)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/discovery/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                    .anyRequest().authenticated())
        .build();
  }

  private static HttpSecurity spaDefaults(HttpSecurity http) throws Exception {
    // Cookie token so the SPA can echo it as X-XSRF-TOKEN; the filter makes Spring write the cookie.
    return http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (req, res, ex) ->
                            error(res, 401, "UNAUTHENTICATED", "Authentication required"))
                    .accessDeniedHandler(
                        (req, res, ex) ->
                            error(
                                res,
                                403,
                                ex instanceof CsrfException ? "CSRF_REJECTED" : "FORBIDDEN",
                                "Request denied")));
  }

  private static void error(HttpServletResponse response, int status, String code, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response
        .getWriter()
        .write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"violations\":[]}");
  }

  private static final class CsrfCookieFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
        HttpServletResponse response, jakarta.servlet.FilterChain chain)
        throws jakarta.servlet.ServletException, IOException {
      CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
      if (token != null) token.getToken(); // loading the deferred token writes the cookie
      chain.doFilter(request, response);
    }
  }
}
