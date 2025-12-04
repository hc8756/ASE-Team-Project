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
 */
@Component
@SuppressWarnings({
    "PMD.SystemPrintln", // Using System.out for simplicity in this example
})
public class LoggerFilter implements Filter {
  
  /** {@inheritDoc} */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    
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