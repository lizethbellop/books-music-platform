package com.musa.music.spotify;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import com.musa.music.entity.MusicContentType;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ArtworkHttpTest {
    @Test void trackDetailSelectsOriginalArtButKeepsUserActionIdentity() {
        var builder=RestClient.builder();
        var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.spotify.com/v1/tracks/source?market=MX")).andRespond(withSuccess("""
            {"id":"source","name":"Song","external_ids":{"isrc":"SEAAA2400001"},
             "artists":[{"id":"artist","name":"Artist"}],
             "album":{"album_type":"compilation","release_date":"2025","artists":[{"id":"various"}],"images":[{"url":"https://example.test/comp.jpg"}]}}
            """,MediaType.APPLICATION_JSON));
        server.expect(queryParam("q","isrc:SEAAA2400001")).andRespond(withSuccess("""
            {"tracks":{"items":[{"id":"original","name":"Song","external_ids":{"isrc":"SEAAA2400001"},
             "artists":[{"id":"artist","name":"Artist"}],
             "album":{"album_type":"album","release_date":"2021","artists":[{"id":"artist"}],"images":[{"url":"https://example.test/original.jpg"}]}}]}}
            """,MediaType.APPLICATION_JSON));
        var auth=mock(SpotifyAuthService.class);
        when(auth.getAccessToken()).thenReturn(java.util.UUID.randomUUID().toString());
        var detail=new SpotifyDetailService(auth,builder.build()).getDetail(MusicContentType.SONG,"source");
        assertEquals("source",detail.spotifyId());
        assertEquals("https://example.test/original.jpg",detail.imageUrl());
        assertEquals("2021",detail.releaseDate());
        server.verify();
    }
}
