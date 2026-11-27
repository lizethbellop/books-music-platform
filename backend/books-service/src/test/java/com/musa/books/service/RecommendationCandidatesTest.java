package com.musa.books.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RecommendationCandidatesTest {
    @Test void retrievesWorkSubjectsInOneRequestWithoutEditionFanOut() {
        var builder = RestClient.builder().baseUrl("https://openlibrary.org");
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(queryParam("limit", "30"))
                .andExpect(queryParam("fields", "key,title,author_name,first_publish_year,cover_i,subject"))
                .andRespond(withSuccess("""
                    {"docs":[
                      {"key":"/works/OL1W","title":"Libro","author_name":["Autora"],"subject":["Fantasy"]},
                      {"key":"/books/OL2M","title":"Edición"}
                    ]}
                    """, MediaType.APPLICATION_JSON));
        var service = new OpenLibraryService(builder.build(), mock(TranslationService.class));
        var result = service.getRecommendationCandidates("subject:Fantasy");
        assertEquals(1, result.size());
        assertEquals("OL1W", result.getFirst().externalId());
        assertEquals("Fantasy", result.getFirst().subjects().getFirst());
        assertNull(result.getFirst().coverUrl());
        server.verify();
    }
}
