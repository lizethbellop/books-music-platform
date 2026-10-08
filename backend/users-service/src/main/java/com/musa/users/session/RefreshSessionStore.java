package com.musa.users.session;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RefreshSessionStore {

    private static final String PREFIX = "RT:v2:";

    private static final DefaultRedisScript<Long> SAVE_SCRIPT =
            new DefaultRedisScript<>("""
                    redis.call('HSET', KEYS[1],
                        'email', ARGV[1],
                        'expiresAt', ARGV[2],
                        'rememberMe', ARGV[3])
                    redis.call('PEXPIREAT', KEYS[1], ARGV[2])
                    return 1
                    """, Long.class);
    private static final DefaultRedisScript<Long> ROTATE_SCRIPT =
            new DefaultRedisScript<>("""
                if redis.call('EXISTS', KEYS[1]) == 0 then
                    return 0
                end

                if redis.call('EXISTS', KEYS[2]) == 1 then
                    return 0
                end

                if redis.call('HGET', KEYS[1], 'email') ~= ARGV[1]
                    or redis.call('HGET', KEYS[1], 'expiresAt') ~= ARGV[2]
                    or redis.call('HGET', KEYS[1], 'rememberMe') ~= ARGV[3] then
                    return 0
                end

                local now = redis.call('TIME')
                local nowMs = tonumber(now[1]) * 1000
                    + math.floor(tonumber(now[2]) / 1000)

                if tonumber(ARGV[2]) <= nowMs then
                    redis.call('DEL', KEYS[1])
                    return 0
                end

                redis.call('HSET', KEYS[2],
                    'email', ARGV[1],
                    'expiresAt', ARGV[2],
                    'rememberMe', ARGV[3])
                redis.call('PEXPIREAT', KEYS[2], ARGV[2])
                redis.call('DEL', KEYS[1])
                return 1
                """, Long.class);

    private final StringRedisTemplate redis;

    public RefreshSessionStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String create(RefreshSession session) {
        if (!session.sessionExpiresAt().isAfter(Instant.now())) {
            throw new IllegalArgumentException("La sesión ya venció.");
        }

        String refreshToken = UUID.randomUUID().toString();

        redis.execute(
                SAVE_SCRIPT,
                List.of(PREFIX + refreshToken),
                session.email(),
                Long.toString(session.sessionExpiresAt().toEpochMilli()),
                Boolean.toString(session.rememberMe())
        );

        return refreshToken;
    }

    public Optional<RefreshSession> find(String refreshToken) {
        String key = PREFIX + refreshToken;
        Map<Object, Object> values = redis.opsForHash().entries(key);

        if (values.isEmpty()) {
            return Optional.empty();
        }

        Instant expiresAt = Instant.ofEpochMilli(
                Long.parseLong(values.get("expiresAt").toString())
        );

        if (!expiresAt.isAfter(Instant.now())) {
            redis.delete(key);
            return Optional.empty();
        }

        return Optional.of(new RefreshSession(
                values.get("email").toString(),
                expiresAt,
                Boolean.parseBoolean(values.get("rememberMe").toString())
        ));
    }

    public void revoke(String refreshToken) {
        redis.delete(PREFIX + refreshToken);
    }

    public Optional<String> rotate(
            String oldRefreshToken,
            RefreshSession session
    ) {
        String newRefreshToken = UUID.randomUUID().toString();

        Long result = redis.execute(
                ROTATE_SCRIPT,
                List.of(
                        PREFIX + oldRefreshToken,
                        PREFIX + newRefreshToken
                ),
                session.email(),
                Long.toString(session.sessionExpiresAt().toEpochMilli()),
                Boolean.toString(session.rememberMe())
        );

        if (Long.valueOf(1L).equals(result)) {
            return Optional.of(newRefreshToken);
        }

        return Optional.empty();
    }
}