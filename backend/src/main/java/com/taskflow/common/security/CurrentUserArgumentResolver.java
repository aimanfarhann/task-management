package com.taskflow.common.security;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@code @CurrentUser AuthenticatedUser} controller parameters from the security context
 * populated by {@link JwtAuthFilter}.
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

  /** Supports parameters of type {@link AuthenticatedUser} annotated with {@link CurrentUser}. */
  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(CurrentUser.class)
        && AuthenticatedUser.class.isAssignableFrom(parameter.getParameterType());
  }

  /**
   * Returns the {@link AuthenticatedUser} principal of the current request.
   *
   * @throws IllegalStateException if no authenticated principal is present — such requests must
   *     have been rejected by the security chain before reaching a controller
   */
  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
      throw new IllegalStateException("No authenticated user in security context");
    }
    return user;
  }
}
