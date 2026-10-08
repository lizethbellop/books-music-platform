package com.musa.users.session;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.Optional;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "MUSA_REDIS_TEST", matches = "true")
class RefreshSessionStoreRedisTest {
    private static LettuceConnectionFactory connection;
    private static StringRedisTemplate redis;
    private static RefreshSessionStore store;
    private final Set<String> tokens = ConcurrentHashMap.newKeySet();

    @BeforeAll
    static void connect() {
        connection = new LettuceConnectionFactory("localhost", 6379);
        connection.afterPropertiesSet();
        redis = new StringRedisTemplate(connection);
        redis.afterPropertiesSet();
        store = new RefreshSessionStore(redis);
    }

    @AfterEach
    void cleanup() {
        tokens.forEach(store::revoke);
    }

    @AfterAll
    static void close() { connection.destroy(); }

    private RefreshSession session() {
        return new RefreshSession("redis-test@example.invalid",
                Instant.now().plusSeconds(60).truncatedTo(ChronoUnit.MILLIS), true);
    }

    private String create(RefreshSession session) {
        String token = store.create(session);
        tokens.add(token);
        return token;
    }

    @Test
    void rotatesOnceAndPreservesExpiry() {
        RefreshSession session = session();
        String old = create(session);
        assertEquals(session, store.find(old).orElseThrow());
        String next = store.rotate(old, session).orElseThrow();
        tokens.add(next);
        assertTrue(store.find(old).isEmpty());
        assertTrue(store.rotate(old, session).isEmpty());
        assertEquals(session, store.find(next).orElseThrow());
        Long ttl = redis.getExpire("RT:v2:" + next, TimeUnit.MILLISECONDS);
        assertNotNull(ttl);
        assertTrue(ttl > 0 && ttl <= 60000);
    }

    @Test
    void logoutMakesRenewalImpossibleAndIsRepeatable() {
        RefreshSession session = session();
        String token = create(session);
        store.revoke(token);
        store.revoke(token);
        assertTrue(store.find(token).isEmpty());
        assertTrue(store.rotate(token, session).isEmpty());
    }

    @Test
    void expiredRedisKeyCannotRenew() {
        RefreshSession session = session();
        String token = create(session);
        redis.expireAt("RT:v2:" + token, Instant.now().minusSeconds(1));
        assertTrue(store.find(token).isEmpty());
        assertTrue(store.rotate(token, session).isEmpty());
    }

    @Test
    void simultaneousRequestsHaveExactlyOneWinner() throws Exception {
        RefreshSession session = session();
        String old = create(session);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Optional<String>> attempt = () -> {
            start.await();
            Optional<String> result = store.rotate(old, session);
            result.ifPresent(tokens::add);
            return result;
        };
        try {
            Future<Optional<String>> a = pool.submit(attempt);
            Future<Optional<String>> b = pool.submit(attempt);
            start.countDown();
            Optional<String> first = a.get(5, TimeUnit.SECONDS);
            Optional<String> second = b.get(5, TimeUnit.SECONDS);
            assertEquals(1, (first.isPresent() ? 1 : 0) + (second.isPresent() ? 1 : 0));
            assertTrue(store.find(old).isEmpty());
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
