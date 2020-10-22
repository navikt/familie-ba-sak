CREATE TABLE OPPLYSNINGSPLIKT
(
    ID               BIGINT PRIMARY KEY,
    FK_BEHANDLING_ID BIGINT REFERENCES behandling (id)   NOT NULL,
    STATUS           VARCHAR,
    BEGRUNNELSE      TEXT,
    VERSJON          BIGINT       DEFAULT 0              NOT NULL,
    OPPRETTET_AV     VARCHAR      DEFAULT 'VL'           NOT NULL,
    OPPRETTET_TID    TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    ENDRET_AV        VARCHAR,
    ENDRET_TID       TIMESTAMP(3)
);

CREATE SEQUENCE OPPLYSNINGSPLIKT_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;