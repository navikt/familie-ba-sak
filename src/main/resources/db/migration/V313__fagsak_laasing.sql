CREATE TABLE IF NOT EXISTS fagsak_laasing
(
    id            BIGINT       NOT NULL PRIMARY KEY,
    fk_fagsak_id  BIGINT       NOT NULL REFERENCES fagsak (id),
    tidspunkt     TIMESTAMP(3) NOT NULL,
    hendelse      VARCHAR      NOT NULL CHECK (hendelse IN ('LÅST', 'LÅST_OPP')),
    begrunnelse   TEXT         NOT NULL,
    aktiv         BOOLEAN      NOT NULL,
    versjon       BIGINT       NOT NULL DEFAULT 0,
    opprettet_av  VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3)
);

CREATE SEQUENCE IF NOT EXISTS fagsak_laasing_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;

CREATE INDEX fagsak_laasing_fagsak_id_idx ON fagsak_laasing (fk_fagsak_id);

-- En fagsak kan kun ha én aktiv låsing om gangen
CREATE UNIQUE INDEX fagsak_laasing_aktiv_unik_idx
    ON fagsak_laasing (fk_fagsak_id)
    WHERE aktiv = true;
