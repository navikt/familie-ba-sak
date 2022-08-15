CREATE TABLE KORRIGERT_ETTERBETALING
(
    ID               BIGINT PRIMARY KEY,
    AARSAK           VARCHAR                             NOT NULL,
    BEGRUNNELSE      VARCHAR,
    BELOP            BIGINT                              NOT NULL,
    AKTIV            BOOLEAN                             NOT NULL,
    FK_BEHANDLING_ID BIGINT REFERENCES BEHANDLING (ID)   NOT NULL,

    -- Base entitet felter
    VERSJON          BIGINT       DEFAULT 0              NOT NULL,
    OPPRETTET_AV     VARCHAR      DEFAULT 'VL'           NOT NULL,
    OPPRETTET_TID    TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    ENDRET_AV        VARCHAR,
    ENDRET_TID       TIMESTAMP(3)
);

CREATE SEQUENCE korrigert_etterbetaling_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE UNIQUE INDEX UIDX_KORRIGERT_ETTERBETALING_FK_BEHANDLING_ID_AKTIV ON KORRIGERT_ETTERBETALING (FK_BEHANDLING_ID) where AKTIV=true;
CREATE INDEX on KORRIGERT_ETTERBETALING (fk_behandling_id);