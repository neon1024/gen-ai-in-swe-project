package com.ubb.genai.events.authoring;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/** Defers replaceable defaults until ordinary host bean definitions have been registered. */
@Configuration(proxyBeanMethods = false)
@ImportAutoConfiguration(AuthoringDefaultsConfiguration.class)
public class AuthoringConfiguration {}
