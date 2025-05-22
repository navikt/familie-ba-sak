CREATE TABLE soknad_referanse
(
    id               BIGINT PRIMARY KEY,
    journalpost_id   VARCHAR                             NOT NULL,
    fk_behandling_id BIGINT                              NOT NULL,
    versjon          BIGINT       DEFAULT 0              NOT NULL,
    opprettet_av     VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid    TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av        VARCHAR,
    endret_tid       TIMESTAMP(3),

    CONSTRAINT soknad_referanse_fk_behandling_id_fkey
        FOREIGN KEY (fk_behandling_id) REFERENCES behandling (id) ON DELETE CASCADE,
    CONSTRAINT unique_soknad_referanse_fk_behandling_id UNIQUE (fk_behandling_id)
);

CREATE SEQUENCE soknad_referanse_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;
