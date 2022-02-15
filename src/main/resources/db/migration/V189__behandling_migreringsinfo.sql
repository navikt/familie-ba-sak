CREATE TABLE BEHANDLING_MIGRERINGSINFO
(
    ID               BIGINT PRIMARY KEY,
    FK_BEHANDLING_ID BIGINT REFERENCES BEHANDLING (ID)   NOT NULL,
    MIGRERINGSDATO   DATE                                NOT NULL,
    VERSJON          BIGINT       DEFAULT 0              NOT NULL,
    OPPRETTET_AV     VARCHAR      DEFAULT 'VL'           NOT NULL,
    OPPRETTET_TID    TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    ENDRET_AV        VARCHAR,
    ENDRET_TID       TIMESTAMP(3)
);

CREATE SEQUENCE BEHANDLING_MIGRERINGSINFO_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE INDEX ON BEHANDLING_MIGRERINGSINFO (FK_BEHANDLING_ID);