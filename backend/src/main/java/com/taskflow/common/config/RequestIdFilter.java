package com.taskflow.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns each request a short id, exposes it in the {@code X-Request-Id} response header, and puts
 * it on the logging MDC so every log line of a request is correlatable (ARCHITECTURE.md §5).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

  private static final String MDC_KEY = "requestId";
  private static final String HEADER_NAME = "X-Request-Id";

  /** Wraps the request with an MDC-scoped request id. */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestId = UUID.randomUUID().toString().substring(0, 8);
    MDC.put(MDC_KEY, requestId);
    response.setHeader(HEADER_NAME, requestId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }
}
