package com.taskflow.common.config;

import com.taskflow.common.security.CurrentUserArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers MVC customizations — currently only the {@code @CurrentUser} argument resolver. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final CurrentUserArgumentResolver currentUserArgumentResolver;

  /**
   * Creates the configuration.
   *
   * @param currentUserArgumentResolver resolves {@code @CurrentUser} controller parameters
   */
  public WebConfig(CurrentUserArgumentResolver currentUserArgumentResolver) {
    this.currentUserArgumentResolver = currentUserArgumentResolver;
  }

  /** Adds the {@code @CurrentUser} resolver to the MVC argument resolvers. */
  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(currentUserArgumentResolver);
  }
}
