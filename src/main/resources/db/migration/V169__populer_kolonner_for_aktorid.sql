insert into aktoer_id(aktoer_id,
                      opprettet_av,
                      opprettet_tid,
                      endret_av,
                      endret_tid)
select distinct
    on (aktoer_id) aktoer_id,
                      opprettet_av,
                      opprettet_tid,
                      'VL',
                      localtimestamp
from po_person ppy
order by aktoer_id, opprettet_tid desc;

insert into personident(foedselsnummer,
                        fk_aktoer_id,
                        aktiv,
                        gjelder_til,
                        opprettet_av,
                        opprettet_tid,
                        endret_av,
                        endret_tid)
select distinct
    on (person_ident) person_ident,
                      aktoer_id,
                      case when
                                   opprettet_tid = (select max(ppi.opprettet_tid) from po_person ppi where ppi.aktoer_id = ppy.aktoer_id ) then true
                           else false
                          end,
                      null,
                      opprettet_av,
                      opprettet_tid,
                      'VL',
                      localtimestamp
from po_person ppy
order by person_ident, opprettet_tid desc;

alter table FAGSAK_PERSON rename column AKTOER_ID to FK_AKTOER_ID;
alter table FAGSAK_PERSON
    add constraint FK_FAGSAK_PERSON foreign key (FK_AKTOER_ID) references AKTOER_ID (AKTOER_ID);

alter table ANDEL_TILKJENT_YTELSE rename column AKTOER_ID to FK_AKTOER_ID;
alter table ANDEL_TILKJENT_YTELSE
    add constraint FK_ANDEL_TILKJENT_YTELSE foreign key (FK_AKTOER_ID) references AKTOER_ID (AKTOER_ID);

alter table PERSON_RESULTAT rename column AKTOER_ID to FK_AKTOER_ID;
alter table PERSON_RESULTAT
    add constraint FK_PERSON_RESULTAT foreign key (FK_AKTOER_ID) references AKTOER_ID (AKTOER_ID);

alter table GR_PERIODE_OVERGANGSSTONAD rename column AKTOER_ID to FK_AKTOER_ID;
alter table GR_PERIODE_OVERGANGSSTONAD
    add constraint FK_GR_PERIODE_OVERGANGSSTONAD foreign key (FK_AKTOER_ID) references AKTOER_ID (AKTOER_ID);

alter table FOEDSELSHENDELSE_PRE_LANSERING rename column AKTOER_ID to FK_AKTOER_ID;
alter table FOEDSELSHENDELSE_PRE_LANSERING
    add constraint FK_FOEDSELSHENDELSE_PRE_LANSERING foreign key (FK_AKTOER_ID) references AKTOER_ID (AKTOER_ID);

alter table PO_PERSON rename column AKTOER_ID to FK_AKTOER_ID;
alter table PO_PERSON
    add constraint FK_PO_PERSON foreign key (FK_AKTOER_ID) references AKTOER_ID (AKTOER_ID);


update fagsak_person fp
set fk_aktoer_id=(select fk_aktoer_id from personident p where p.foedselsnummer = fp.ident);

update andel_tilkjent_ytelse aty
set fk_aktoer_id=(select fk_aktoer_id from personident p where p.foedselsnummer = aty.person_ident);

update person_resultat pr
set fk_aktoer_id=(select fk_aktoer_id from personident p where p.foedselsnummer = pr.person_ident);

update gr_periode_overgangsstonad gpo
set fk_aktoer_id=(select fk_aktoer_id from personident p where p.foedselsnummer = gpo.person_ident);

update foedselshendelse_pre_lansering fpl
set fk_aktoer_id=(select fk_aktoer_id from personident p where p.foedselsnummer = fpl.person_ident);

alter table fagsak add column fk_aktoer_id varchar;
alter table FAGSAK
    add constraint FAGSAK foreign key (FK_AKTOER_ID) references AKTOER_ID (AKTOER_ID);

update fagsak f
set fk_aktoer_id=(select fk_aktoer_id
                  from personident p
                  where p.foedselsnummer =
                        (select ident
                         from fagsak_person fp
                         where fk_fagsak_id = f.id
                           and fp.arkivert = false));

