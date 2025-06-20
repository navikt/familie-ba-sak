CREATE TABLE minside_aktivering
(
    id            BIGINT PRIMARY KEY,
    fk_aktor_id   VARCHAR                             NOT NULL UNIQUE,
    aktivert      BOOLEAN                             NOT NULL,
    versjon       BIGINT       DEFAULT 0              NOT NULL,
    opprettet_av  VARCHAR      DEFAULT 'VL'           NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    endret_av     VARCHAR,
    endret_tid    TIMESTAMP(3),
    FOREIGN KEY (fk_aktor_id) REFERENCES AKTOER (aktoer_id) ON UPDATE CASCADE
);

CREATE SEQUENCE minside_aktivering_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;