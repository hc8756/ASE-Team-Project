package dev.ase.teamproject.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.Instant;

@Component
public class LoggerFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // Get client IP from X-Forwarded-For (Cloud Run) or remote address
        String clientIp = httpRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        
        // Simple log format: IP | METHOD | ENDPOINT | TIMESTAMP
        System.out.println("CLIENT_LOG: " + clientIp + " | " + 
                          httpRequest.getMethod() + " " + 
                          httpRequest.getRequestURI() + " | " + 
                          Instant.now());
        
        // Continue with the request
        chain.doFilter(request, response);
    }
}