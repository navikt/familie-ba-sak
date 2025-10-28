CREATE INDEX IF NOT EXISTS idx_andel_stonad_fom_tom_behandling
    ON andel_tilkjent_ytelse(fk_behandling_id, stonad_fom, stonad_tom);

CREATE INDEX IF NOT EXISTS idx_behandling_fagsak_aktivert
    ON behandling(fk_fagsak_id, aktivert_tid DESC);