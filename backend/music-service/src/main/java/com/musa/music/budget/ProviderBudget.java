package com.musa.music.budget;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProviderBudget {
    private final JdbcTemplate jdbc;
    private final int dailyLimit;
    private final int minuteLimit;
    private static final String PROVIDER = "spotify";
    public static final String RESERVE_SQL = """
        UPDATE external_api_budget SET
          calls=CASE WHEN budget_day=(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date THEN calls+1 ELSE 1 END,
          budget_day=(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date,
          minute_calls=CASE WHEN window_start>CURRENT_TIMESTAMP-interval '1 minute' THEN minute_calls+1 ELSE 1 END,
          window_start=CASE WHEN window_start>CURRENT_TIMESTAMP-interval '1 minute' THEN window_start ELSE CURRENT_TIMESTAMP END
        WHERE provider=? AND blocked_until<=CURRENT_TIMESTAMP
          AND (budget_day<>(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date OR calls<?)
          AND (window_start<=CURRENT_TIMESTAMP-interval '1 minute' OR minute_calls<?)
        RETURNING calls
        """;

    public ProviderBudget(JdbcTemplate jdbc,
        @Value("${external.spotify.daily-limit:180}") int dailyLimit,
        @Value("${external.spotify.minute-limit:30}") int minuteLimit) {
        this.jdbc=jdbc;
        this.dailyLimit=dailyLimit;
        this.minuteLimit=minuteLimit;
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void reserve() {
        if (dailyLimit<=0 || minuteLimit<=0 || jdbc.query(RESERVE_SQL,
            (rs, row) -> rs.getInt(1), PROVIDER, dailyLimit, minuteLimit).isEmpty()) {
            throw new BudgetExceeded();
        }
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void cooldown(String retryAfter) {
        long seconds=120;
        try {
            seconds=Long.parseLong(retryAfter);
        } catch (RuntimeException ignored) {
            try {
                seconds=Duration.between(java.time.Instant.now(),java.time.ZonedDateTime.parse(
                    retryAfter,java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).getSeconds();
            } catch (RuntimeException alsoIgnored) { }
        }
        seconds=Math.min(86400,Math.max(60,seconds));
        jdbc.update("UPDATE external_api_budget SET blocked_until=GREATEST(blocked_until,CURRENT_TIMESTAMP+(? * interval '1 second')) WHERE provider=?", seconds,PROVIDER);
    }

    public static class BudgetExceeded extends ResponseStatusException {
        public BudgetExceeded() {
            super(HttpStatus.SERVICE_UNAVAILABLE,"La consulta externa está temporalmente limitada. Los datos guardados siguen disponibles.");
        }
        @Override public HttpHeaders getHeaders() {
            HttpHeaders headers=new HttpHeaders();
            headers.set(HttpHeaders.RETRY_AFTER,"60");
            return headers;
        }
    }
}
