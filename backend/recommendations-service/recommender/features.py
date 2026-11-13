import unicodedata

def normalize_label(label:str) ->str:
    text = label.casefold().strip()

    text = unicodedata.normalize("NFKD", text)

    text = "".join(
        character
        for character in text 
        if not unicodedata.combining(character)
    
    )

    return " ".join(text.split())

SUBJECT_ALIASES = {
    "young adult fiction": "young_adult",
    "ficcion juvenil": "young_adult",
    "novelas juveniles": "young_adult",

    "romance": "romance",
    "romance fiction": "romance",
    "novela romantica": "romance",
    "love stories": "romance",

    "magic realism": "magical_realism",
    "magic realism (literature)": "magical_realism",
    "realismo magico": "magical_realism",
}

def normalize_subject(subject: str) -> str:
    label = normalize_label(subject)
    return SUBJECT_ALIASES.get(label, label)