package com.musa.profile.service;

import com.musa.profile.dto.catalog.BookCatalogResponse;
import com.musa.profile.dto.catalog.CatalogResolution;
import com.musa.profile.dto.catalog.MusicCatalogResponse;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CatalogLookupServiceTest {

    private MockRestServiceServer booksServer;
    private MockRestServiceServer musicServer;
    private CatalogLookupService service;

    @BeforeEach
    void setUp() {
        var booksBuilder = RestClient.builder()
                .baseUrl("http://books.test");

        var musicBuilder = RestClient.builder()
                .baseUrl("http://music.test");

        booksServer = MockRestServiceServer
                .bindTo(booksBuilder)
                .build();

        musicServer = MockRestServiceServer
                .bindTo(musicBuilder)
                .build();

        service = new CatalogLookupService(
                booksBuilder.build(),
                musicBuilder.build()
        );
    }

    @AfterEach
    void verifyRequests() {
        booksServer.verify();
        musicServer.verify();
    }

    @Test
    void debeObtenerLibroYEnviarToken() {
        booksServer.expect(requestTo("http://books.test/books/OL1W"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer token-prueba"
                ))
                .andRespond(withSuccess("""
                        {
                          "externalId": "OL1W",
                          "title": "Libro de prueba",
                          "authors": ["Autora"],
                          "coverUrl": "https://example.com/book.jpg"
                        }
                        """, MediaType.APPLICATION_JSON));

        var result = service.resolve("BOOK", "OL1W", "token-prueba");

        assertEquals(
                CatalogResolution.Status.AVAILABLE,
                result.resolutionStatus()
        );

        var book = assertInstanceOf(
                BookCatalogResponse.class,
                result.content()
        );

        assertEquals(
                "https://example.com/book.jpg",
                book.coverUrl()
        );
    }

    @Test
    void debeObtenerCancionYEnviarToken() {
        musicServer.expect(requestTo(
                        "http://music.test/api/music/SONG/song-test"
                ))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer token-prueba"
                ))
                .andRespond(withSuccess("""
                        {
                          "spotifyId": "song-test",
                          "contentType": "SONG",
                          "name": "Canción de prueba",
                          "artistName": "Artista",
                          "imageUrl": "https://example.com/song.jpg"
                        }
                        """, MediaType.APPLICATION_JSON));

        var result = service.resolve(
                "SONG", "song-test", "token-prueba"
        );

        assertEquals(
                CatalogResolution.Status.AVAILABLE,
                result.resolutionStatus()
        );

        var song = assertInstanceOf(
                MusicCatalogResponse.class,
                result.content()
        );

        assertEquals("Artista", song.artistName());
    }

    @Test
    void debeObtenerArtistaSinImagen() {
        musicServer.expect(requestTo(
                        "http://music.test/api/music/ARTIST/artist-test"
                ))
                .andRespond(withSuccess("""
                        {
                          "spotifyId": "artist-test",
                          "contentType": "ARTIST",
                          "name": "Artista de prueba",
                          "imageUrl": null
                        }
                        """, MediaType.APPLICATION_JSON));

        var result = service.resolve(
                "ARTIST", "artist-test", "token-prueba"
        );

        assertEquals(
                CatalogResolution.Status.AVAILABLE,
                result.resolutionStatus()
        );

        var artist = assertInstanceOf(
                MusicCatalogResponse.class,
                result.content()
        );

        assertNull(artist.imageUrl());
    }

    @Test
    void debeDevolverNotFoundCuandoRecibe404() {
        booksServer.expect(requestTo("http://books.test/books/OL1W"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        var result = service.resolve("BOOK", "OL1W", "token-prueba");

        assertEquals(
                CatalogResolution.Status.NOT_FOUND,
                result.resolutionStatus()
        );
        assertNull(result.content());
    }

    @Test
    void debeDevolverUnavailableCuandoElServicioFalla() {
        booksServer.expect(requestTo("http://books.test/books/OL1W"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        var result = service.resolve("BOOK", "OL1W", "token-prueba");

        assertEquals(
                CatalogResolution.Status.UNAVAILABLE,
                result.resolutionStatus()
        );
        assertNull(result.content());
    }

    @Test
    void debeDevolverUnavailableCuandoHayTimeout() {
        booksServer.expect(requestTo("http://books.test/books/OL1W"))
                .andRespond(withException(
                        new SocketTimeoutException("Timeout simulado")
                ));

        var result = service.resolve("BOOK", "OL1W", "token-prueba");

        assertEquals(
                CatalogResolution.Status.UNAVAILABLE,
                result.resolutionStatus()
        );
    }

    @Test
    void debeRechazarRespuestaDeOtroLibro() {
        booksServer.expect(requestTo("http://books.test/books/OL1W"))
                .andRespond(withSuccess("""
                        {
                          "externalId": "OL2W",
                          "title": "Otro libro"
                        }
                        """, MediaType.APPLICATION_JSON));

        var result = service.resolve("BOOK", "OL1W", "token-prueba");

        assertEquals(
                CatalogResolution.Status.UNAVAILABLE,
                result.resolutionStatus()
        );
    }

    @Test
    void debeRechazarRespuestaConTipoMusicalIncorrecto() {
        musicServer.expect(requestTo(
                        "http://music.test/api/music/SONG/song-test"
                ))
                .andRespond(withSuccess("""
                        {
                          "spotifyId": "song-test",
                          "contentType": "ARTIST",
                          "name": "Tipo incorrecto"
                        }
                        """, MediaType.APPLICATION_JSON));

        var result = service.resolve(
                "SONG", "song-test", "token-prueba"
        );

        assertEquals(
                CatalogResolution.Status.UNAVAILABLE,
                result.resolutionStatus()
        );
    }

    @Test
    void noDebeConsultarServiciosParaTiposNoAdmitidos() {
        var result = service.resolve(
                "ALBUM", "album-test", "token-prueba"
        );

        assertEquals(
                CatalogResolution.Status.UNAVAILABLE,
                result.resolutionStatus()
        );
    }
}