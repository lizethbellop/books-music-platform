INSERT INTO music_content (
    spotify_id,
    content_type,
    name,
    artist_name,
    image_url
    created_at
)
VALUES

(
    'seed_artist_cris_mj',
    'ARTIST',
    'Cris MJ',
    'Cris MJ',
    NULL
    CURRENT_TIMESTAMP
),
(
    'seed_song_una_noche_en_medellin',
    'SONG',
    'Una Noche en Medellín',
    'Cris MJ',
    NULL
    CURRENT_TIMESTAMP
),
(
    'seed_song_gata_only',
    'SONG',
    'Gata Only',
    'Cris MJ',
    NULL
    CURRENT_TIMESTAMP
),
(
    'seed_song_dejame_pensar',
    'SONG',
    'Déjame Pensar',
    'Cris MJ',
    NULL
    CURRENT_TIMESTAMP
),

(
    'seed_artist_chuyin',
    'ARTIST',
    'Chuyin',
    'Chuyin',
    NULL
    CURRENT_TIMESTAMP
),
(
    'seed_song_pues_ya_ni_pedo',
    'SONG',
    'Pues Ya Ni Pedo',
    'Chuyin',
    NULL
    CURRENT_TIMESTAMP
),
(
    'seed_song_inmortal',
    'SONG',
    'Inmortal',
    'Chuyin',
    NULL
    CURRENT_TIMESTAMP
),
(
    'seed_song_casaditas',
    'SONG',
    'Casaditas',
    'Chuyin',
    NULL
    CURRENT_TIMESTAMP
),

(
    'seed_artist_omar_courtz',
    'ARTIST',
    'Omar Courtz',
    'Omar Courtz',
    NULL
    CURRENT_TIMESTAMP
),
(
    'seed_song_2k16',
    'SONG',
    '2K16',
    'Omar Courtz',
    NULL
    CURRENT_TIMESTAMP
),
(
    'seed_song_luces_de_colores',
    'SONG',
    'Luces de Colores',
    'Omar Courtz',
    NULL
    CURRENT_TIMESTAMP
),

(
    'seed_artist_bad_gyal',
    'ARTIST',
    'Bad Gyal',
    'Bad Gyal',
    NULL
    CURRENT_TIMESTAMP
),
(
    'seed_song_fiebre',
    'SONG',
    'Fiebre',
    'Bad Gyal',
    NULL
    CURRENT_TIMESTAMP
),
(
    'seed_song_chulo',
    'SONG',
    'Chulo',
    'Bad Gyal',
    NULL
    CURRENT_TIMESTAMP
),
(
    'seed_album_la_joia',
    'ALBUM',
    'La Joia',
    'Bad Gyal',
    NULL
    CURRENT_TIMESTAMP
),

(
    'seed_artist_soda_stereo',
    'ARTIST',
    'Soda Stereo',
    'Soda Stereo',
    NULL
    CURRENT_TIMESTAMP
),
(
    'seed_song_de_musica_ligera',
    'SONG',
    'De Música Ligera',
    'Soda Stereo',
    NULL
    CURRENT_TIMESTAMP
),
(
    'seed_song_persiana_americana',
    'SONG',
    'Persiana Americana',
    'Soda Stereo',
    NULL
    CURRENT_TIMESTAMP
),
(
    'seed_song_corazon_delator',
    'SONG',
    'Corazón Delator',
    'Soda Stereo',
    NULL
    CURRENT_TIMESTAMP
),
(
    'seed_album_cancion_animal',
    'ALBUM',
    'Canción Animal',
    'Soda Stereo',
    NULL
    CURRENT_TIMESTAMP
),
(
    'seed_album_signos',
    'ALBUM',
    'Signos',
    'Soda Stereo',
    NULL
    CURRENT_TIMESTAMP
)

ON CONFLICT (spotify_id, content_type) DO NOTHING;