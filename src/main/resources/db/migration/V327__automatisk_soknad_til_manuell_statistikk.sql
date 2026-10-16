CREATE TABLE automatisk_behandling_av_soknad_statistikk
(
    id                            bigint primary key,
    behandling_id                 bigint,
    journalpost_id                bigint,
    aarsak_til_manuell_behandling VARCHAR(255),
    opprettet_tid                 TIMESTAMP(3) DEFAULT localtimestamp
);

CREATE SEQUENCE automatisk_behandling_av_soknad_statistikk_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;
