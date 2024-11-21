CREATE TABLE BEREGNING_RESULTAT
(
    ID                 bigint primary key,
    FK_BEHANDLING_ID   bigint references BEHANDLING (id),
    STONAD_FOM         timestamp,
    STONAD_TOM         timestamp not null,
    OPPRETTET_DATO     timestamp not null,
    OPPHOR_FOM         timestamp,
    UTBETALINGSOPPDRAG text      not null
);

CREATE SEQUENCE BEREGNING_RESULTAT_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE INDEX ON BEREGNING_RESULTAT (FK_BEHANDLING_ID);