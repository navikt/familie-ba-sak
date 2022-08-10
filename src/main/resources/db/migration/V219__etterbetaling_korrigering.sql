CREATE TABLE ETTERBETALING_KORRIGERING
(
    ID               BIGINT PRIMARY KEY,
    AARSAK           VARCHAR                             NOT NULL,
    BEGRUNNELSE      VARCHAR,
    BELOP            BIGINT                              NOT NULL,
    FK_BEHANDLING_ID BIGINT REFERENCES BEHANDLING (ID)   NOT NULL,
    OPPRETTET_AV     VARCHAR      DEFAULT 'VL'           NOT NULL,
    OPPRETTET_TID    TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    AKTIV            BOOLEAN                             NOT NULL
);

CREATE SEQUENCE etterbetaling_korrigering_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;