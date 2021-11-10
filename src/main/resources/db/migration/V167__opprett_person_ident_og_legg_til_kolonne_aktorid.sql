create table PERSONIDENT
(
    ID                  BIGINT      PRIMARY KEY,
    AKTOER_ID           VARCHAR                             NOT NULL,
    FOEDSELSNUMMER      VARCHAR                             NOT NULL,
    AKTIV               BOOLEAN     DEFAULT FALSE           NOT NULL,
    GJELDER_TIL         TIMESTAMP(3),
    VERSJON             BIGINT       DEFAULT 0              NOT NULL,
    OPPRETTET_AV        VARCHAR      DEFAULT 'VL'           NOT NULL,
    OPPRETTET_TID       TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    ENDRET_AV           VARCHAR,
    ENDRET_TID          TIMESTAMP(3)
);

create sequence PERSONIDENT_SEQ increment by 50 start with 1000000 NO CYCLE;

alter table FAGSAK_PERSON
    add column AKTOER_ID VARCHAR;

alter table ANDEL_TILKJENT_YTELSE
    add column AKTOER_ID VARCHAR;

alter table PERSON_RESULTAT
    add column AKTOER_ID VARCHAR;

alter table OKONOMI_SIMULERING_MOTTAKER
    add column AKTOER_ID VARCHAR;

alter table GR_PERIODE_OVERGANGSSTONAD
    add column AKTOER_ID VARCHAR;

alter table FOEDSELSHENDELSE_PRE_LANSERING
    add column AKTOER_ID VARCHAR;

