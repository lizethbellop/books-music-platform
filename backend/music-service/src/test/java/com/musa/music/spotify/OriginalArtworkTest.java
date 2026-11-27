package com.musa.music.spotify;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class OriginalArtworkTest {
    private Map track(String id,String isrc,String artist,String type,String date,String image) {
        return Map.of("id",id,"external_ids",Map.of("isrc",isrc),"artists",List.of(Map.of("id",artist)),
            "album",Map.of("album_type",type,"release_date",date,"artists",List.of(Map.of("id",type.equals("compilation")?"various":artist)),"images",List.of(Map.of("url",image))));
    }
    @Test void compilationUsesEarliestArtistReleaseWithSameRecording() {
        Map source=track("comp","SEAAA2400001","artist","compilation","2025","comp.jpg");
        Map original=track("original","SEAAA2400001","artist","album","2021","original.jpg");
        Map later=track("later","SEAAA2400001","artist","album","2024","later.jpg");
        assertTrue(OriginalArtwork.needsOriginal(source));
        assertSame(original,OriginalArtwork.choose(source,List.of(later,source,original)));
    }
    @Test void anotherRecordingCannotReplaceArtworkEvenWithSameArtist() {
        Map source=track("comp","SEAAA2400001","artist","compilation","2025","comp.jpg");
        Map wrong=track("wrong","SEAAA2400002","artist","album","2021","wrong.jpg");
        assertSame(source,OriginalArtwork.choose(source,List.of(wrong)));
    }
    @Test void anotherArtistCannotReplaceArtworkEvenWithMatchingIdentifier() {
        Map source=track("comp","SEAAA2400001","artist","compilation","2025","comp.jpg");
        Map wrong=track("wrong","SEAAA2400001","other","album","2021","wrong.jpg");
        assertSame(source,OriginalArtwork.choose(source,List.of(wrong)));
    }
    @Test void officialArtistReleaseNeedsNoExtraSearch() {
        assertFalse(OriginalArtwork.needsOriginal(track("official","SEAAA2400001","artist","album","2021","original.jpg")));
    }
}
