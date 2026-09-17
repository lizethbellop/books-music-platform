package com.musa.music.spotify;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class SpotifyAuthService {

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.token-url}")
    private String tokenUrl;

    private final RestClient restClient = RestClient.create();

    public String getAccessToken() {

        String credentials = clientId + ":" + clientSecret;

        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        Map response = restClient.post()
                .uri(tokenUrl)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Basic " + encodedCredentials
                )
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("access_token") == null) {
            throw new RuntimeException(
                    "No se pudo obtener el token de Spotify"
            );
        }

        return response.get("access_token").toString();
    }
}