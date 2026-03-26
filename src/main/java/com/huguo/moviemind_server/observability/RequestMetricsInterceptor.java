package com.huguo.moviemind_server.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.concurrent.TimeUnit;

@Component
public class RequestMetricsInterceptor implements HandlerInterceptor {

    private static final String START_NANO_ATTR = "requestStartNano";

    private final MeterRegistry meterRegistry;

    public RequestMetricsInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_NANO_ATTR, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long startNano = (Long) request.getAttribute(START_NANO_ATTR);
        if (startNano == null) {
            return;
        }

        long elapsed = System.nanoTime() - startNano;
        String endpoint = normalizeEndpoint(request);
        String method = request.getMethod();
        String status = String.valueOf(response.getStatus());

        Timer.builder("moviemind_http_server_requests_duration")
                .description("HTTP request duration for MovieMind endpoints")
                .tag("method", method)
                .tag("endpoint", endpoint)
                .tag("status", status)
                .register(meterRegistry)
                .record(elapsed, TimeUnit.NANOSECONDS);

        Counter.builder("moviemind_http_server_requests_total")
                .description("HTTP request count for MovieMind endpoints")
                .tag("method", method)
                .tag("endpoint", endpoint)
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }

    private String normalizeEndpoint(HttpServletRequest request) {
        Object bestMatchPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (bestMatchPattern instanceof String pattern && !pattern.isBlank()) {
            return pattern;
        }
        String uri = request.getRequestURI();
        return uri == null || uri.isBlank() ? "unknown" : uri;
    }
}
