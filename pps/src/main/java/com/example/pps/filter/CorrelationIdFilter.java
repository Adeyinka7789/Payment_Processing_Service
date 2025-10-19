package com.example.pps.filter;

import com.example.pps.context.RequestContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CorrelationIdFilter implements Filter {

    private static final String HEADER_NAME = "X-Correlation-ID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Check if client sent a correlation ID
        String correlationId = httpRequest.getHeader(HEADER_NAME);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = RequestContext.generateCorrelationId();
        }

        // Store in ThreadLocal and MDC (for log4j/slf4j context)
        RequestContext.setCorrelationId(correlationId);
        MDC.put("correlationId", correlationId);

        try {
            chain.doFilter(request, response);
        } finally {
            // Clean up
            RequestContext.clear();
            MDC.clear();
        }
    }
}