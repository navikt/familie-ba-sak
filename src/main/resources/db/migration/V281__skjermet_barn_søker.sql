CREATE TABLE skjermet_barn_soker
(
    id            BIGINT PRIMARY KEY,
    fk_aktor_id   VARCHAR REFERENCES AKTOER (AKTOER_ID) ON UPDATE CASCADE NOT NULL,
    versjon       BIGINT       DEFAULT 0                                  NOT NULL,
    opprettet_av  VARCHAR      DEFAULT 'VL'                               NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT localtimestamp                     NOT NULL,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3)
);

CREATE SEQUENCE skjermet_barn_soker_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;

ALTER TABLE FAGSAK
    ADD COLUMN fk_skjermet_barn_soker_id BIGINT;

ALTER TABLE FAGSAK
    ADD FOREIGN KEY (fk_skjermet_barn_soker_id) REFERENCES skjermet_barn_soker (id);
