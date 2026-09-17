package com.musa.music.spotify;

import com.musa.music.dto.MusicDetailResponse;
import com.musa.music.entity.MusicContentType;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class SpotifyDetailService {

    private final SpotifyAuthService spotifyAuthService;
    private final RestClient restClient = RestClient.create();

    public SpotifyDetailService(
            SpotifyAuthService spotifyAuthService
    ) {
        this.spotifyAuthService = spotifyAuthService;
    }

    public MusicDetailResponse getDetail(
            MusicContentType contentType,
            String spotifyId
    ) {

        String token = spotifyAuthService.getAccessToken();

        return switch (contentType) {
            case SONG -> getTrackDetail(token, spotifyId);
            case ALBUM -> getAlbumDetail(token, spotifyId);
            case ARTIST -> getArtistDetail(token, spotifyId);
        };
    }

    private MusicDetailResponse getTrackDetail(
            String token,
            String spotifyId
    ) {

        Map response = restClient.get()
                .uri("https://api.spotify.com/v1/tracks/{id}?market=MX", spotifyId)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new RuntimeException("No se encontró la canción");
        }

        String name = (String) response.get("name");
        String artistName = getFirstArtistName(response);

        Map album = (Map) response.get("album");

        String imageUrl = getFirstImage(album);
        String releaseDate = album != null
                ? (String) album.get("release_date")
                : null;

        String spotifyUrl = getSpotifyUrl(response);

        return new MusicDetailResponse(
                spotifyId,
                MusicContentType.SONG,
                name,
                artistName,
                imageUrl,
                spotifyUrl,
                releaseDate,
                null
        );
    }

    private MusicDetailResponse getAlbumDetail(
            String token,
            String spotifyId
    ) {

        Map response = restClient.get()
                .uri("https://api.spotify.com/v1/albums/{id}?market=MX", spotifyId)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new RuntimeException("No se encontró el álbum");
        }

        String name = (String) response.get("name");
        String artistName = getFirstArtistName(response);
        String imageUrl = getFirstImage(response);
        String releaseDate = (String) response.get("release_date");
        Integer totalTracks = (Integer) response.get("total_tracks");
        String spotifyUrl = getSpotifyUrl(response);

        return new MusicDetailResponse(
                spotifyId,
                MusicContentType.ALBUM,
                name,
                artistName,
                imageUrl,
                spotifyUrl,
                releaseDate,
                totalTracks
        );
    }

    private MusicDetailResponse getArtistDetail(
            String token,
            String spotifyId
    ) {

        Map response = restClient.get()
                .uri("https://api.spotify.com/v1/artists/{id}", spotifyId)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new RuntimeException("No se encontró el artista");
        }

        String name = (String) response.get("name");
        String imageUrl = getFirstImage(response);
        String spotifyUrl = getSpotifyUrl(response);

        return new MusicDetailResponse(
                spotifyId,
                MusicContentType.ARTIST,
                name,
                name,
                imageUrl,
                spotifyUrl,
                null,
                null
        );
    }

    private String getFirstArtistName(Map item) {

        List<Map> artists = (List<Map>) item.get("artists");

        if (artists == null || artists.isEmpty()) {
            return null;
        }

        return (String) artists.get(0).get("name");
    }

    private String getFirstImage(Map item) {

        if (item == null) {
            return null;
        }

        List<Map> images = (List<Map>) item.get("images");

        if (images == null || images.isEmpty()) {
            return null;
        }

        return (String) images.get(0).get("url");
    }

    private String getSpotifyUrl(Map item) {

        Map externalUrls = (Map) item.get("external_urls");

        if (externalUrls == null) {
            return null;
        }

        return (String) externalUrls.get("spotify");
    }
}