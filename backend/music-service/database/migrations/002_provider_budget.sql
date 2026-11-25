-- No recrear base/usuario. Ejecutar conectado como propietario del servicio.
CREATE TABLE IF NOT EXISTS external_api_budget (
    provider VARCHAR(30) PRIMARY KEY,
    budget_day DATE NOT NULL,
    calls INTEGER NOT NULL DEFAULT 0 CHECK (calls>=0),
    window_start TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    minute_calls INTEGER NOT NULL DEFAULT 0 CHECK (minute_calls>=0),
    blocked_until TIMESTAMPTZ NOT NULL DEFAULT 'epoch'
);
INSERT INTO external_api_budget(provider,budget_day,window_start)
VALUES ('spotify',(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date,CURRENT_TIMESTAMP-interval '1 minute')
ON CONFLICT (provider) DO NOTHING;
