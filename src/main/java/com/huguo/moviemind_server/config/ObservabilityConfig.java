package com.huguo.moviemind_server.config;

import com.huguo.moviemind_server.observability.RequestMetricsInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ObservabilityConfig implements WebMvcConfigurer {

    private final RequestMetricsInterceptor requestMetricsInterceptor;

    public ObservabilityConfig(RequestMetricsInterceptor requestMetricsInterceptor) {
        this.requestMetricsInterceptor = requestMetricsInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestMetricsInterceptor);
    }
}
