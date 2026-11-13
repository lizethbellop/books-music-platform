package com.musa.music.config;
import com.musa.music.budget.ProviderBudget;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;

@Configuration
public class SpotifyRestClientConfig {
    @Bean
    public RestClient spotifyRestClient(ProviderBudget budget) {
        var client=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(java.time.Duration.ofSeconds(5)).build();
        var factory=new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(java.time.Duration.ofSeconds(15));
        return RestClient.builder().requestFactory(factory)
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
