CREATE TABLE sammensatt_kontrollsak
(
    id               BIGINT PRIMARY KEY,
    fk_behandling_id BIGINT REFERENCES BEHANDLING (ID)   NOT NULL,
    fritekst         VARCHAR,
    versjon          BIGINT       DEFAULT 0              NOT NULL,
    opprettet_av     VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid    TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av        VARCHAR,
    endret_tid       TIMESTAMP(3)
);

CREATE SEQUENCE sammensatt_kontrollsak_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE UNIQUE INDEX sammensatt_kontrollsak_seq_fk_behandling_id_idx ON sammensatt_kontrollsak (fk_behandling_id);

