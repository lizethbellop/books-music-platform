package com.musa.music.spotify;

import com.musa.music.dto.MusicSearchItemResponse;
import com.musa.music.entity.MusicContentType;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SpotifySearchService {

    private final SpotifyAuthService spotifyAuthService;
    private final RestClient restClient = RestClient.create();

    public SpotifySearchService(SpotifyAuthService spotifyAuthService) {
        this.spotifyAuthService = spotifyAuthService;
    }

    public List<MusicSearchItemResponse> search(String query) {

        String token = spotifyAuthService.getAccessToken();

        Map response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.spotify.com")
                        .path("/v1/search")
                        .queryParam("q", query)
                        .queryParam("type", "track,album,artist")
                        .queryParam("market", "MX")
                        .queryParam("limit", 5)
                        .build())
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .retrieve()
                .body(Map.class);

        List<MusicSearchItemResponse> results = new ArrayList<>();

        if (response == null) {
            return results;
        }

        extractTracks(response, results);
        extractAlbums(response, results);
        extractArtists(response, results);

        return results;
    }

    private void extractTracks(
            Map response,
            List<MusicSearchItemResponse> results
    ) {

        Map tracks = (Map) response.get("tracks");

        if (tracks == null) {
            return;
        }

        List<Map> items = (List<Map>) tracks.get("items");

        if (items == null) {
            return;
        }

        for (Map track : items) {

            String spotifyId = (String) track.get("id");
            String name = (String) track.get("name");

            String artistName = getFirstArtistName(track);

            Map album = (Map) track.get("album");
            String imageUrl = getFirstImage(album);

            results.add(
                    new MusicSearchItemResponse(
                            spotifyId,
                            MusicContentType.SONG,
                            name,
                            artistName,
                            imageUrl
                    )
            );
        }
    }

    private void extractAlbums(
            Map response,
            List<MusicSearchItemResponse> results
    ) {

        Map albums = (Map) response.get("albums");

        if (albums == null) {
            return;
        }

        List<Map> items = (List<Map>) albums.get("items");

        if (items == null) {
            return;
        }

        for (Map album : items) {

            String spotifyId = (String) album.get("id");
            String name = (String) album.get("name");

            String artistName = getFirstArtistName(album);
            String imageUrl = getFirstImage(album);

            results.add(
                    new MusicSearchItemResponse(
                            spotifyId,
                            MusicContentType.ALBUM,
                            name,
                            artistName,
                            imageUrl
                    )
            );
        }
    }

    private void extractArtists(
            Map response,
            List<MusicSearchItemResponse> results
    ) {

        Map artists = (Map) response.get("artists");

        if (artists == null) {
            return;
        }

        List<Map> items = (List<Map>) artists.get("items");

        if (items == null) {
            return;
        }

        for (Map artist : items) {

            String spotifyId = (String) artist.get("id");
            String name = (String) artist.get("name");

            String imageUrl = getFirstImage(artist);

            results.add(
                    new MusicSearchItemResponse(
                            spotifyId,
                            MusicContentType.ARTIST,
                            name,
                            name,
                            imageUrl
                    )
            );
        }
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
}