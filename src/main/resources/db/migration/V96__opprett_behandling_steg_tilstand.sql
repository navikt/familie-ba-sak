DROP TABLE IF EXISTS BEHANDLING_STEG_TILSTAND

CREATE TABLE BEHANDLING_STEG_TILSTAND
(
    id                      BIGINT                            primary key,
    fk_behandling_id        BIGINT references BEHANDLING(id)     not null,
    behandling_steg         VARCHAR                              not null,
    utfort                  BOOLEAN      default false           not null,
    versjon                 BIGINT       default 0               not null,
    opprettet_av            VARCHAR      default 'VL'            not null,
    opprettet_tid           TIMESTAMP(3) default localtimestamp  not null,
    endret_av               VARCHAR,
    endret_tid              TIMESTAMP(3)
) ;

CREATE SEQUENCE BEHANDLING_STEG_TILSTAND_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
