CREATE INDEX IF NOT EXISTS idx_behandling_fagsak_aktivert
    ON behandling(fk_fagsak_id, aktivert_tid DESC);