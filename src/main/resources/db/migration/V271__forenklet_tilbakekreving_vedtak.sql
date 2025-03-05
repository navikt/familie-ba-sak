CREATE TABLE forenklet_tilbakekreving_vedtak
(
    id               BIGINT PRIMARY KEY,
    fk_behandling_id BIGINT                              NOT NULL,
    samtykke         BOOLEAN,
    fritekst         VARCHAR      DEFAULT false          NOT NULL,
    versjon          BIGINT       DEFAULT 0              NOT NULL,
    opprettet_av     VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid    TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av        VARCHAR,
    endret_tid       TIMESTAMP(3),

    CONSTRAINT fk_forenklet_tilbakekreving_behandling
        FOREIGN KEY (fk_behandling_id) REFERENCES behandling (id) ON DELETE CASCADE,
    CONSTRAINT unique_fk_behandling_id UNIQUE (fk_behandling_id)
);

CREATE SEQUENCE forenklet_tilbakekreving_vedtak_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;

