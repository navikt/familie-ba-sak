CREATE TABLE IF NOT EXISTS PATCHET_ANDEL_TILKJENT_YTELSE
(
    id                              bigint primary key,
    behandling_id                   bigint                              not null,
    tilkjent_ytelse_id              bigint                              not null,
    aktoer_id                       bigint                              not null,
    kalkulert_utbetalingsbelop      numeric,
    stonad_fom                      TIMESTAMP(3)                        not null,
    stonad_tom                      TIMESTAMP(3)                        not null,
    type                            varchar(50)                         not null,
    sats                            bigint,
    prosent                         numeric,
    kilde_behandling_id             bigint,
    periode_offset                  bigint,
    forrige_periode_offset          bigint,
    nasjonalt_periodebelop          numeric,
    differanseberegnet_periodebelop numeric,
    versjon                         bigint       default 0              not null,
    opprettet_av                    VARCHAR(512) default 'VL'           not null,
    opprettet_tid                   TIMESTAMP(3) default localtimestamp not null,
    endret_av                       VARCHAR(512),
    endret_tid                      TIMESTAMP(3)
);