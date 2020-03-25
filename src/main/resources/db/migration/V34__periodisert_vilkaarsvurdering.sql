CREATE TABLE BEHANDLING_RESULTAT
(
    ID               BIGINT PRIMARY KEY,
    FK_BEHANDLING_ID BIGINT REFERENCES behandling (id)   NOT NULL,
    AKTIV            BOOLEAN      DEFAULT TRUE           NOT NULL,
    VERSJON          BIGINT       DEFAULT 0              NOT NULL,
    OPPRETTET_AV     VARCHAR      DEFAULT 'VL'           NOT NULL,
    OPPRETTET_TID    TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    ENDRET_AV        VARCHAR,
    ENDRET_TID       TIMESTAMP(3)
);

alter table behandling
    rename column resultat to brev_type;

alter table vilkar_resultat
    rename constraint vilkar_resultat_samlet_vilkar_resultat_id_fkey to vilkar_resultat_periode_resultat_id_fkey;

alter table samlet_vilkar_resultat
    drop column fk_behandling_id;
alter table samlet_vilkar_resultat
    rename to periode_resultat;
alter table periode_resultat
    add column behandling_resultat_id bigint references BEHANDLING_RESULTAT (id) default null;
alter table periode_resultat
    add column person_ident varchar default '' not null;
alter table periode_resultat
    add column periode_fom timestamp(3);
alter table periode_resultat
    add column periode_tom timestamp(3);
alter index samlet_vilkar_resultat_pkey rename to periode_resultat_pkey;