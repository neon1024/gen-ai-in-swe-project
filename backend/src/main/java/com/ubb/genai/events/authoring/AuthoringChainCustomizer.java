package com.ubb.genai.events.authoring;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/** Lets a host (or the dev stubs) add filters to the authoring security chain. */
public interface AuthoringChainCustomizer {
  void customize(HttpSecurity http) throws Exception;
}
