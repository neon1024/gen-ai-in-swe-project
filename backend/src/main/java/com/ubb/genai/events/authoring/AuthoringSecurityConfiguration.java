package com.ubb.genai.events.authoring;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;

@Configuration(proxyBeanMethods = false)
public class AuthoringSecurityConfiguration {
  @Bean
  @Order(1)
  SecurityFilterChain authoringSecurity(HttpSecurity http) throws Exception {
    return http.securityMatcher("/api/event-authoring/**")
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
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
                                "Request denied")))
        .build();
  }

  @Bean
  @Order(2147483647)
  SecurityFilterChain baselineSecurity(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .formLogin(org.springframework.security.config.Customizer.withDefaults())
        .httpBasic(org.springframework.security.config.Customizer.withDefaults())
        .build();
  }

  private static void error(HttpServletResponse response, int status, String code, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response
        .getWriter()
        .write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\",\"violations\":[]}");
  }
}
