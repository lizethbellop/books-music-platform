BEGIN;

INSERT INTO moderation_terms (
    term,
    normalized_term
)
VALUES
    ('idiota', 'idiota'),
    ('imbécil', 'imbecil'),
    ('estúpido', 'estupido')
ON CONFLICT (normalized_term) DO NOTHING;


INSERT INTO moderation_term_variants (
    term_id,
    variant,
    normalized_variant
)
SELECT id, '1d10ta', 'idiota'
FROM moderation_terms
WHERE normalized_term = 'idiota'
ON CONFLICT (normalized_variant) DO NOTHING;


INSERT INTO moderation_term_variants (
    term_id,
    variant,
    normalized_variant
)
SELECT id, 'imb3cil', 'imbecil'
FROM moderation_terms
WHERE normalized_term = 'imbecil'
ON CONFLICT (normalized_variant) DO NOTHING;


INSERT INTO moderation_term_variants (
    term_id,
    variant,
    normalized_variant
)
SELECT id, 'estup1do', 'estupido'
FROM moderation_terms
WHERE normalized_term = 'estupido'
ON CONFLICT (normalized_variant) DO NOTHING;

COMMIT;