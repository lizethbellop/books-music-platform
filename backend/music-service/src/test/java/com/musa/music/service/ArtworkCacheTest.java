package com.musa.music.service;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import com.musa.music.entity.*;
import com.musa.music.repository.MusicContentRepository;
import com.musa.music.spotify.*;
import com.musa.music.dto.MusicDetailResponse;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ArtworkCacheTest {
    @Test void oldArtworkIsRecheckedAndCachedWithoutChangingRecordingId() {
        var repository=mock(MusicContentRepository.class);
        var spotify=mock(SpotifyDetailService.class);
        var auth=mock(SpotifyAuthService.class);
        var local=MusicContent.builder().spotifyId("existing").contentType(MusicContentType.SONG)
            .name("Song").imageUrl("compilation.jpg").build();
        when(repository.findBySpotifyIdAndContentType("existing",MusicContentType.SONG)).thenReturn(Optional.of(local));
        when(auth.isConfigured()).thenReturn(true);
        when(spotify.getDetail(MusicContentType.SONG,"existing")).thenReturn(new MusicDetailResponse("existing",MusicContentType.SONG,"Song","Artist","original.jpg",null,null,null));
        var service=new MusicDetailService(repository,spotify,auth);
        assertEquals("original.jpg",service.getDetail(MusicContentType.SONG,"existing").imageUrl());
        assertEquals("original.jpg",service.getDetail(MusicContentType.SONG,"existing").imageUrl());
        assertEquals("existing",local.getSpotifyId());
        assertNotNull(local.getArtworkCheckedAt());
        verify(spotify,times(1)).getDetail(MusicContentType.SONG,"existing");
        verify(repository,times(1)).save(local);
    }
}
