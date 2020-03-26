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

CREATE SEQUENCE BEHANDLING_RESULTAT_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
insert into BEHANDLING_RESULTAT (id, fk_behandling_id, aktiv, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid)
select nextval('BEHANDLING_RESULTAT_SEQ'), fk_behandling_id, aktiv, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid from samlet_vilkar_resultat;

alter table samlet_vilkar_resultat add column person_ident varchar;
update samlet_vilkar_resultat svr set person_ident=(select distinct p.person_ident from po_person p, vilkar_resultat vr where p.id = vr.fk_person_id and vr.samlet_vilkar_resultat_id=svr.id limit 1);

alter table samlet_vilkar_resultat add column behandling_resultat_id bigint references BEHANDLING_RESULTAT (id) default null;
update samlet_vilkar_resultat svr set behandling_resultat_id=(select distinct br.id from behandling_resultat br, samlet_vilkar_resultat svr where svr.fk_behandling_id = br.fk_behandling_id);

alter table behandling
    rename column resultat to brev_type;

alter table samlet_vilkar_resultat
    drop column fk_behandling_id,
    drop column aktiv;

alter table samlet_vilkar_resultat
    rename to periode_resultat;
alter table periode_resultat
    add column periode_fom timestamp(3),
    add column periode_tom timestamp(3);
alter index samlet_vilkar_resultat_pkey rename to periode_resultat_pkey;

alter table vilkar_resultat
    rename constraint vilkar_resultat_samlet_vilkar_resultat_id_fkey to vilkar_resultat_periode_resultat_id_fkey;
alter table vilkar_resultat
    rename column samlet_vilkar_resultat_id to periode_resultat_id;
alter table vilkar_resultat
    drop column fk_person_id;