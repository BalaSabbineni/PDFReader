package com.pdfreader.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Auth endpoints: max 10 requests per minute (brute force protection)
    private final ConcurrentHashMap<String, Bucket> authBuckets = new ConcurrentHashMap<>();

    // API endpoints: max 100 requests per minute per IP
    private final ConcurrentHashMap<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ip = getClientIp(request);
        String path = request.getRequestURI();

        Bucket bucket = path.startsWith("/api/auth/")
                ? authBuckets.computeIfAbsent(ip, k -> newAuthBucket())
                : apiBuckets.computeIfAbsent(ip, k -> newApiBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Too many requests. Please slow down.\"}");
        }
    }

    private Bucket newAuthBucket() {
        // 10 requests per minute — protects against brute force login/register
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(10)
                        .refillGreedy(10, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private Bucket newApiBucket() {
        // 50 requests per minute — general protection
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(50)
                        .refillGreedy(50, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        // ngrok sets X-Forwarded-For with the real client IP
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
