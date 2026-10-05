package com.manmohan.contactapi.config;

import java.util.List;

import com.manmohan.contactapi.AppProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Only the portfolio origin(s) may call the API from a browser.
 * Implemented as a servlet filter that runs first, so even 429/400 responses carry CORS headers
 * and the browser can show the real error instead of an opaque network failure.
 */
@Configuration
public class CorsConfig {

    @Bean
    FilterRegistrationBean<CorsFilter> corsFilter(AppProperties props) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(props.cors().allowedOrigins());
        cfg.setAllowedMethods(List.of("POST", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Content-Type"));
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/api/**", cfg);
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(src));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
