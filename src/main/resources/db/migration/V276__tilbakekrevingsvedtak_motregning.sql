drop table forenklet_tilbakekrevingsvedtak;

CREATE TABLE tilbakekrevingsvedtak_motregning
(
    id               BIGINT PRIMARY KEY,
    fk_behandling_id BIGINT                              NOT NULL,
    samtykke         BOOLEAN      DEFAULT false,
    fritekst         VARCHAR                             NOT NULL,
    vedtak_pdf       BYTEA,
    versjon          BIGINT       DEFAULT 0              NOT NULL,
    opprettet_av     VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid    TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av        VARCHAR,
    endret_tid       TIMESTAMP(3),

    CONSTRAINT fk_tilbakekrevingsvedtak_motregning
        FOREIGN KEY (fk_behandling_id) REFERENCES behandling (id) ON DELETE CASCADE,
    CONSTRAINT unique_fk_behandling_id UNIQUE (fk_behandling_id)
);

CREATE SEQUENCE tilbakekrevingsvedtak_motregning_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;

