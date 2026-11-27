package com.musa.books.budget;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ProviderBudgetTest {
    @Test void zeroBudgetPreventsEvenAttemptingReservation() {
        var jdbc=mock(JdbcTemplate.class);
        var budget=new ProviderBudget(jdbc,0,10);
        assertThrows(ProviderBudget.BudgetExceeded.class,budget::reserve);
        verifyNoInteractions(jdbc);
    }
    @Test void exhaustedCounterReturnsRetryAfter() {
        var jdbc=mock(JdbcTemplate.class);
        when(jdbc.query(eq(ProviderBudget.RESERVE_SQL), org.mockito.ArgumentMatchers.<RowMapper<Integer>>any(),
            eq("openlibrary"),eq(2),eq(3))).thenReturn(List.of());
        var error=assertThrows(ProviderBudget.BudgetExceeded.class,()->new ProviderBudget(jdbc,2,3).reserve());
        assertEquals(503,error.getStatusCode().value());
        assertEquals("60",error.getHeaders().getFirst("Retry-After"));
    }
    @Test void budgetDenialNeverReachesProvider() {
        var budget=mock(ProviderBudget.class);
        doThrow(new ProviderBudget.BudgetExceeded()).when(budget).reserve();
        var builder=new com.musa.books.config.RestClientConfig().openLibraryRestClient("https://openlibrary.org",budget).mutate();
        var server=MockRestServiceServer.bindTo(builder).build();
        RestClient client=builder.build();
        assertThrows(ProviderBudget.BudgetExceeded.class,()->client.get().uri("https://example.test/never").retrieve().toBodilessEntity());
        server.verify();
        verify(budget).reserve();
    }
    @Test void upstream429ActivatesSharedCooldown() {
        var budget=mock(ProviderBudget.class);
        var builder=new com.musa.books.config.RestClientConfig().openLibraryRestClient("https://openlibrary.org",budget).mutate();
        var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://example.test/limited"))
            .andRespond(withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS).header("Retry-After","90"));
        assertThrows(org.springframework.web.client.HttpClientErrorException.class,
            ()->builder.build().get().uri("https://example.test/limited").retrieve().toBodilessEntity());
        server.verify();
        verify(budget).reserve();
        verify(budget).cooldown("90");
    }
}
