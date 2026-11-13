-- Presupuestos exclusivos de recommendations-service, compartidos por sus procesos.
INSERT INTO book_provider_budget(provider,budget_day,calls,next_call_at)
VALUES ('groq',(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date,0,CURRENT_TIMESTAMP),
       ('openlibrary',(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date,0,CURRENT_TIMESTAMP),
       ('spotify',(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date,0,CURRENT_TIMESTAMP)
ON CONFLICT (provider) DO NOTHING;
