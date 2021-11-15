insert into personident(foedselsnummer,
                        aktoer_id,
                        id,
                        aktiv,
                        gjelder_til,
                        endret_av,
                        endret_tid)
select distinct
on (person_ident) person_ident,
    aktoer_id,
    nextval('personident_seq'),
    true,
    null,
    'VL',
    localtimestamp
from po_person
order by person_ident, aktoer_id;

update fagsak_person fp
set aktoer_id=(select aktoer_id from personident p where p.foedselsnummer = fp.ident);

update andel_tilkjent_ytelse aty
set aktoer_id=(select aktoer_id from personident p where p.foedselsnummer = aty.person_ident);

update person_resultat pr
set aktoer_id=(select aktoer_id from personident p where p.foedselsnummer = pr.person_ident);

update gr_periode_overgangsstonad gpo
set aktoer_id=(select aktoer_id from personident p where p.foedselsnummer = gpo.person_ident);

update foedselshendelse_pre_lansering fpl
set aktoer_id=(select aktoer_id from personident p where p.foedselsnummer = fpl.person_ident);

alter table fagsak
    add column aktoer_id varchar;

update fagsak f
set aktoer_id=(select aktoer_id
               from personident p
               where p.foedselsnummer =
                     (select ident
                      from fagsak_person fp
                      where fk_fagsak_id = f.id
                        and fp.arkivert = false));

