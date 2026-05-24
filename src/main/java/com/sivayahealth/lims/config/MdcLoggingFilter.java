package com.sivayahealth.lims.config;

import com.sivayahealth.lims.security.LimsUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Populates Log4j2 ThreadContext (MDC) for every request.
 * Fields appear in every log line: traceId, tenantId, userId, httpMethod, requestUri
 */
@Component
public class MdcLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Honour X-Cloud-Trace-Context from GCP load balancer, else generate one
            String traceId = request.getHeader("X-Cloud-Trace-Context");
            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString().replace("-", "");
            } else {
                // Format: TRACE_ID/SPAN_ID;o=TRACE_TRUE — extract trace part
                traceId = traceId.split("[/;]")[0];
            }

            ThreadContext.put("traceId",    traceId);
            ThreadContext.put("httpMethod", request.getMethod());
            ThreadContext.put("requestUri", request.getRequestURI());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof LimsUserDetails u) {
                ThreadContext.put("tenantId", String.valueOf(u.getTenantId()));
                ThreadContext.put("userId",   String.valueOf(u.getUser().getId()));
                ThreadContext.put("username", u.getUsername());
            }

            response.setHeader("X-Trace-Id", traceId);
            filterChain.doFilter(request, response);
        } finally {
            ThreadContext.clearAll();
        }
    }
}
