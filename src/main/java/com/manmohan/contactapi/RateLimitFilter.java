package com.manmohan.contactapi;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Sliding-window limiter per client IP on POST /api/contact (in-memory; fine for a single instance). */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final int max;
    private final long windowMs;
    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public RateLimitFilter(AppProperties props) {
        this.max = props.rateLimit().maxRequests();
        this.windowMs = Duration.ofMinutes(props.rateLimit().windowMinutes()).toMillis();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && request.getRequestURI().equals("/api/contact"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        long now = System.currentTimeMillis();
        Deque<Long> q = hits.computeIfAbsent(req.getRemoteAddr(), k -> new ArrayDeque<>());
        long retryAfterSec = 0;
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > windowMs) q.pollFirst();
            if (q.size() >= max) retryAfterSec = Math.max(1, (windowMs - (now - q.peekFirst())) / 1000);
            else q.addLast(now);
        }
        if (retryAfterSec > 0) {
            res.setStatus(429);
            res.setHeader("Retry-After", String.valueOf(retryAfterSec));
            res.setContentType("application/json");
            res.getWriter().write("{\"success\":false,\"message\":\"Too many messages. Please try again later.\"}");
            return;
        }
        if (hits.size() > 10_000) hits.entrySet().removeIf(e -> e.getValue().isEmpty());
        chain.doFilter(req, res);
    }
}
