package dev.ase.teamproject.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Logs all incoming API requests with client IP, method, endpoint, and timestamp.
 * The client IP is determined from the X-Forwarded-For header if available, otherwise
 * from the remote address.
 */
@Component
public class LoggerFilter implements Filter {

  /**
   * Logs the details of an incoming HTTP request and passes it along the filter chain.
   * Extracts client IP from X-Forwarded-For header or falls back to remote address,
   * then logs the IP, HTTP method, request URI, and current timestamp to standard output.
   *
   * @param request The incoming servlet request.
   * @param response The servlet response.
   * @param chain The filter chain to continue processing the request.
   * @throws IOException if an I/O error occurs during filtering.
   * @throws ServletException if a servlet error occurs during filtering.
   */
  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response,
                       final FilterChain chain)
      throws IOException, ServletException {
    final HttpServletRequest httpRequest = (HttpServletRequest) request;

    String clientIp = httpRequest.getHeader("X-Forwarded-For");
    if (clientIp == null || clientIp.isEmpty()) {
      clientIp = request.getRemoteAddr();
    }

    // Simple log format: IP | METHOD | ENDPOINT | TIMESTAMP
    System.out.println("CLIENT_LOG: " + clientIp + " | "
        + httpRequest.getMethod() + " "
        + httpRequest.getRequestURI() + " | "
        + Instant.now());

    chain.doFilter(request, response);
  }
}