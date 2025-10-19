package com.example.pps.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.codec.ByteArrayCodec;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;

public class RateLimitFilter extends OncePerRequestFilter {

    private final ProxyManager<String> proxyManager;
    private final RedisClient redisClient;

    public RateLimitFilter(String redisUrl) {
        // Parse the Redis URL
        RedisURI redisURI = RedisURI.create(redisUrl);

        // Create Redis client with the URI
        this.redisClient = RedisClient.create(redisURI);

        // Create a composite codec for <String, byte[]>
        RedisCodec<String, byte[]> codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);

        // Connect using the codec
        StatefulRedisConnection<String, byte[]> connection = redisClient.connect(codec);

        // Create Redis-backed ProxyManager
        this.proxyManager = LettuceBasedProxyManager
                .builderFor(connection)
                .build();
    }

    /**
     * Resolves or creates a new bucket for a given key based on a standard configuration.
     * @param key The key (API Key or IP address).
     * @return The resolved Bucket object.
     */
    private io.github.bucket4j.Bucket resolveBucket(String key) {
        // Define the rate limit: 10 requests per 1 minute.
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));

        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(limit)
                .build();

        // Build the bucket using the proxy manager's builder
        return proxyManager.builder().build(key, configuration);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String apiKey = request.getHeader("x-api-key");
        String key = (apiKey != null && !apiKey.isBlank())
                ? "apiKey:" + apiKey
                : "ip:" + request.getRemoteAddr();

        io.github.bucket4j.Bucket bucket = resolveBucket(key);

        if (bucket.tryConsume(1)) {
            // Request allowed
            filterChain.doFilter(request, response);
        } else {
            // Request rate limited
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("""
                {
                  "error": "Too Many Requests",
                  "message": "Rate limit exceeded. Try again later."
                }
            """);
        }
    }

    /**
     * Clean up Redis connection when filter is destroyed
     */
    @Override
    public void destroy() {
        if (redisClient != null) {
            redisClient.shutdown();
        }
        super.destroy();
    }
}