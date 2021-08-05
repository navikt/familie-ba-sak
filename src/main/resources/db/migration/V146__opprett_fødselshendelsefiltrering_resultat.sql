CREATE TABLE FOEDSELSHENDELSEFILTRERING_RESULTAT
(
    ID                  BIGINT PRIMARY KEY,
    FK_BEHANDLING_ID    BIGINT REFERENCES BEHANDLING (ID)   NOT NULL,
    FILTRERINGSREGEL    VARCHAR                             NOT NULL,
    RESULTAT            VARCHAR                             NOT NULL,
    BEGRUNNELSE         TEXT                                NOT NULL,
    EVALUERINGSAARSAKER TEXT                                NOT NULL,
    REGEL_INPUT         TEXT,
    VERSJON             BIGINT       DEFAULT 0              NOT NULL,
    OPPRETTET_AV        VARCHAR      DEFAULT 'VL'           NOT NULL,
    OPPRETTET_TID       TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    ENDRET_AV           VARCHAR,
    ENDRET_TID          TIMESTAMP(3)
);

CREATE SEQUENCE FOEDSELSHENDELSEFILTRERING_RESULTAT_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE INDEX ON FOEDSELSHENDELSEFILTRERING_RESULTAT (FK_BEHANDLING_ID);

UPDATE BEHANDLING_STEG_TILSTAND SET behandling_steg = 'HENLEGG_BEHANDLING' where behandling_steg = 'HENLEGG_SØKNAD';
