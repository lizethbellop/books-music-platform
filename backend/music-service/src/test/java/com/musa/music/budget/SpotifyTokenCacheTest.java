package com.musa.music.budget;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.test.util.ReflectionTestUtils;
import com.musa.music.spotify.SpotifyAuthService;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import org.springframework.http.MediaType;

class SpotifyTokenCacheTest {
    @Test void repeatedRequestsReuseUnexpiredToken() {
        var builder=RestClient.builder();
        var server=MockRestServiceServer.bindTo(builder).build();
        String token=java.util.UUID.randomUUID().toString();
        server.expect(requestTo("https://example.test/token")).andRespond(withSuccess(
            "{\"access_token\":\""+token+"\",\"expires_in\":3600}",MediaType.APPLICATION_JSON));
        var auth=new SpotifyAuthService(builder.build());
        ReflectionTestUtils.setField(auth,"clientId",java.util.UUID.randomUUID().toString());
        ReflectionTestUtils.setField(auth,"clientSecret",java.util.UUID.randomUUID().toString());
        ReflectionTestUtils.setField(auth,"tokenUrl","https://example.test/token");
        assertEquals(token,auth.getAccessToken());
        assertEquals(token,auth.getAccessToken());
        server.verify();
    }
}
