CREATE TABLE FAGSAK
(
    ID            bigint primary key,
    SAKSNUMMER    varchar(19)   not null unique,
    AKTOER_ID     VARCHAR(50)   not null,
    PERSON_IDENT  VARCHAR(50)   not null,
    VERSJON       bigint        DEFAULT 0,
    OPPRETTET_AV  VARCHAR(20)   DEFAULT 'VL',
    OPPRETTET_TID TIMESTAMP(3)  DEFAULT localtimestamp,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);
CREATE SEQUENCE FAGSAK_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
create index on FAGSAK (SAKSNUMMER);
create index on FAGSAK (AKTOER_ID);
create index on FAGSAK (PERSON_IDENT);

COMMENT ON COLUMN FAGSAK.saksnummer is 'Saksnummeret som saken er journalført på';
COMMENT ON COLUMN FAGSAK.AKTOER_ID is 'Søker som har stilt kravet';


CREATE TABLE BEHANDLING
(
    ID                  bigint primary key,
    FK_FAGSAK_ID        bigint references FAGSAK (id),
    VERSJON             bigint       DEFAULT 0,
    OPPRETTET_AV        VARCHAR(20)  DEFAULT 'VL',
    OPPRETTET_TID       TIMESTAMP(3) DEFAULT localtimestamp,
    ENDRET_AV           VARCHAR(20),
    ENDRET_TID          TIMESTAMP(3),
    JOURNALPOST_ID      VARCHAR(50)
);

create index on BEHANDLING (SAK_ID);
CREATE SEQUENCE BEHANDLING_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;

