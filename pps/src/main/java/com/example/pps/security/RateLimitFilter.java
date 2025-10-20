package com.example.pps.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

public class RateLimitFilter extends OncePerRequestFilter {

    private final ProxyManager<String> proxyManager;
    private final RedisClient redisClient;

    public RateLimitFilter(String redisUrl) {
        // ✅ Create Redis client and connection
        RedisURI redisURI = RedisURI.create(redisUrl);
        this.redisClient = RedisClient.create(redisURI);

        // ✅ Create a Redis codec manually — no CompositeCodec needed
        RedisCodec<String, byte[]> codec = RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);

        // ✅ Connect using the codec
        StatefulRedisConnection<String, byte[]> connection = redisClient.connect(codec);

        // ✅ Create a distributed Bucket4j ProxyManager
        this.proxyManager = LettuceBasedProxyManager.builderFor(connection).build();
    }

    /**
     * Resolves or creates a new bucket for a given key based on a standard configuration.
     */
    private io.github.bucket4j.Bucket resolveBucket(String key) {
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));

        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(limit)
                .build();

        // ✅ Supplier form (required for newer Bucket4j versions)
        return proxyManager.builder().build(key, () -> configuration);
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
            filterChain.doFilter(request, response);
        } else {
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

    @Override
    public void destroy() {
        if (redisClient != null) {
            redisClient.shutdown();
        }
        super.destroy();
    }
}