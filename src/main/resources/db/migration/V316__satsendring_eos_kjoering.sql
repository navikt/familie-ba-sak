CREATE TABLE IF NOT EXISTS satsendring_eos_kjoering
(
    id               BIGINT      NOT NULL PRIMARY KEY,
    fk_fagsak_id     BIGINT      NOT NULL REFERENCES fagsak (id) ON DELETE CASCADE,
    fk_behandling_id BIGINT REFERENCES behandling (id) ON DELETE SET NULL,
    utbetalingsland VARCHAR NOT NULL,
    sats_tid         DATE        NOT NULL,
    feiltype         VARCHAR,
    start_tid        TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    ferdig_tid       TIMESTAMP(3),
    CONSTRAINT satsendring_eos_kjoering_fagsak_land_sats_unique UNIQUE (fk_fagsak_id, utbetalingsland, sats_tid)
);

CREATE UNIQUE INDEX IF NOT EXISTS satsendring_eos_kjoering_behandling_id_unique_idx
    ON satsendring_eos_kjoering (fk_behandling_id)
    WHERE fk_behandling_id IS NOT NULL;

CREATE SEQUENCE IF NOT EXISTS satsendring_eos_kjoering_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;
