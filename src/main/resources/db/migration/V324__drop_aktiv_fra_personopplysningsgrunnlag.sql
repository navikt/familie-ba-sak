CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS gr_personopplysninger_behandling_unik_idx
    ON gr_personopplysninger (fk_behandling_id);

ALTER TABLE gr_personopplysninger DROP COLUMN aktiv;
