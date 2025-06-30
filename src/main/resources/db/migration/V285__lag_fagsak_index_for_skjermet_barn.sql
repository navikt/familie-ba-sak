CREATE UNIQUE INDEX IF NOT EXISTS ny_uidx_fagsak_type_aktoer_ikke_arkivert ON fagsak (type, fk_aktoer_id)
    WHERE fagsak.fk_institusjon_id IS NULL
        AND fagsak.fk_skjermet_barn_soker_id IS NULL
        AND arkivert = false;

CREATE UNIQUE INDEX IF NOT EXISTS uidx_fagsak_type_aktoer_skjermet_barn_ikke_arkivert ON fagsak (type, fk_aktoer_id, fk_skjermet_barn_soker_id)
    WHERE fagsak.fk_skjermet_barn_soker_id IS NOT NULL
        AND arkivert = false;