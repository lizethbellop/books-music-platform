package com.musa.music.controller;

import com.musa.music.spotify.SpotifyAuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/music/spotify")
public class SpotifyTestController {

    private final SpotifyAuthService spotifyAuthService;

    public SpotifyTestController(
            SpotifyAuthService spotifyAuthService
    ) {
        this.spotifyAuthService = spotifyAuthService;
    }

    @GetMapping("/test")
    public String testSpotify() {
        String token = spotifyAuthService.getAccessToken();

        return "Conexión con Spotify correcta. Token obtenido.";
    }
}