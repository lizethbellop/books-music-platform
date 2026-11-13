package com.musa.music.spotify;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Presentation only: never supplies Spotify metadata to recommendation vectors. */
public final class OriginalArtwork {
    private OriginalArtwork() {}

    public static String isrc(Map track) {
        Object ids=track.get("external_ids");
        return ids instanceof Map<?,?> values && values.get("isrc") instanceof String value ? value : "";
    }

    private static Set<String> artistIds(Map value) {
        Set<String> ids=new HashSet<>();
        if(value.get("artists") instanceof List<?> artists) {
            for(Object row:artists) if(row instanceof Map<?,?> artist && artist.get("id") instanceof String id) ids.add(id);
        }
        return ids;
    }

    public static boolean needsOriginal(Map track) {
        if(!(track.get("album") instanceof Map album)) return false;
        if("compilation".equals(album.get("album_type"))) return true;
        Set<String> albumArtists=artistIds(album);
        Set<String> trackArtists=artistIds(track);
        return !albumArtists.isEmpty() && !trackArtists.isEmpty() && java.util.Collections.disjoint(albumArtists,trackArtists);
    }

    public static Map choose(Map track,List<Map> candidates) {
        String recording=isrc(track);
        if(recording.isBlank()) return track;
        Set<String> artists=artistIds(track);
        return candidates.stream().filter(candidate -> recording.equalsIgnoreCase(isrc(candidate)))
            .filter(candidate -> !artists.isEmpty() && artistIds(candidate).equals(artists))
            .filter(candidate -> candidate.get("album") instanceof Map album
                && ("album".equals(album.get("album_type")) || "single".equals(album.get("album_type")))
                && !java.util.Collections.disjoint(artistIds(album),artists)
                && album.get("images") instanceof List<?> images && !images.isEmpty())
            .min(Comparator.comparing((Map candidate)->String.valueOf(((Map)candidate.get("album")).getOrDefault("release_date","9999")))
                .thenComparing(candidate->String.valueOf(candidate.get("id"))))
            .orElse(track);
    }
}
