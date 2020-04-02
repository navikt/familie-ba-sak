alter table vilkar_resultat
    add column begrunnelse TEXT;

update vilkar_resultat vr
set begrunnelse=periode_resultat_begrunnelse.begrunnelse
FROM (WITH behandling_resultat_begrunnelse AS (
    SELECT br.id, b.begrunnelse
    FROM behandling_resultat br,
         behandling b
    WHERE br.FK_BEHANDLING_ID = b.id)
                 SELECT brb.begrunnelse, pr.id as periode_resultat_id
                 from behandling_resultat_begrunnelse brb
                          inner join periode_resultat pr on pr.fk_behandling_resultat_id = brb.ID) as periode_resultat_begrunnelse

where periode_resultat_begrunnelse.periode_resultat_id = vr.fk_periode_resultat_id;

alter table behandling
    drop column begrunnelse;