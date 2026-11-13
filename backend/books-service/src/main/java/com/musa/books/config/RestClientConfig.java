package com.musa.books.config;
import com.musa.books.budget.ProviderBudget;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;

@Configuration
public class RestClientConfig {
    @Bean
    public RestClient openLibraryRestClient(@org.springframework.beans.factory.annotation.Value("${openlibrary.api-url}") String apiUrl, ProviderBudget budget) {
        var client=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(java.time.Duration.ofSeconds(5)).build();
        var factory=new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(java.time.Duration.ofSeconds(15));
        return RestClient.builder().baseUrl(apiUrl).requestFactory(factory)
            .requestInterceptor((request,body,execution) -> {
                budget.reserve();
                var response=execution.execute(request,body);
                if (response.getStatusCode().value()==429) {
                    try { budget.cooldown(response.getHeaders().getFirst("Retry-After")); }
                    catch (RuntimeException error) { response.close(); throw error; }
                }
                return response;
            }).build();
    }
}
