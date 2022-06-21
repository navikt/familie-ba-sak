create TABLE INSTITUSJON
(
    ID              BIGINT                              PRIMARY KEY,
    ORG_NUMMER      VARCHAR NOT NULL,
    TSS_EKSTERN_ID  VARCHAR NOT NULL,
    VERSJON         bigint       default 0              not null,
    OPPRETTET_AV    VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID   TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV       VARCHAR(20),
    ENDRET_TID      TIMESTAMP(3)
);

create sequence institusjon_seq increment by 50 start with 1000000 NO CYCLE;

CREATE UNIQUE INDEX uidx_institusjon_org_nummer ON institusjon(org_nummer);
CREATE UNIQUE INDEX uidx_institusjon_tss_ekstern_id ON institusjon(tss_ekstern_id)