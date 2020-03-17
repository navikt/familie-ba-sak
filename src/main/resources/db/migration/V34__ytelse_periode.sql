create table YTELSE_PERIODE
(
    id                      bigint                                                  primary key,
    fk_vedtak_person_id     bigint       references vedtak_person (id)              not null,
    belop                   numeric,
    stonad_fom              TIMESTAMP(3)                                            not null,
    stonad_tom              TIMESTAMP(3)                                            not null,
    type                   VARCHAR(50),
    opprettet_av            VARCHAR(20)  default 'VL'                               not null,
    opprettet_tid           TIMESTAMP(3) default localtimestamp                     not null,
    endret_av               VARCHAR(20),
    endret_tid              TIMESTAMP(3),
    versjon                 bigint       default 0                                  not null
);

CREATE SEQUENCE YTELSE_PERIODE_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
create index on YTELSE_PERIODE (fk_vedtak_person_id);
