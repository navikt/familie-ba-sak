CREATE TABLE ecbvalutakurscache
(
    ID             BIGINT                              NOT NULL PRIMARY KEY,
    VALUTAKURSDATO TIMESTAMP(3) DEFAULT null,
    VALUTAKODE     VARCHAR      DEFAULT null,
    KURS           DECIMAL      DEFAULT null,
    VERSJON        BIGINT       DEFAULT 0              NOT NULL,
    OPPRETTET_AV   VARCHAR      DEFAULT 'VL'           NOT NULL,
    OPPRETTET_TID  TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    ENDRET_AV      VARCHAR,
    ENDRET_TID     TIMESTAMP(3)
);

CREATE SEQUENCE ecbvalutakurscache_seq INCREMENT BY 50 START WITH 1 NO CYCLE;
CREATE INDEX valutakode_valutadato_idx ON ecbvalutakurscache (VALUTAKURSDATO, VALUTAKODE);