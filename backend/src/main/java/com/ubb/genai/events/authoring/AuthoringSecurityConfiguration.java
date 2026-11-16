package com.ubb.genai.events.authoring;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.ObjectProvider;
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

/**
 * Secures only the authoring routes. Global security for other modules belongs to the identity
 * share; this module must not lock or open routes it does not own (decision K-8).
 */
@Configuration(proxyBeanMethods = false)
public class AuthoringSecurityConfiguration {
  @Bean
  @Order(1)
  SecurityFilterChain authoringSecurity(
      HttpSecurity http, ObjectProvider<AuthoringChainCustomizer> customizer) throws Exception {
    // Cookie token so the SPA can echo it as X-XSRF-TOKEN; the filter makes Spring write the cookie.
    http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);
    var extra = customizer.getIfAvailable();
    if (extra != null) extra.customize(http);
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
