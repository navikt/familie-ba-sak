CREATE TABLE OPPGAVE (
    id bigint primary key,
    fk_behandling_id bigint,
    gsak_id varchar(50) not null,
    type varchar(100) not null,
    ferdigstilt bool not null,
    opprettet_tidspunkt timestamp not null
);

CREATE SEQUENCE OPPGAVE_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;

