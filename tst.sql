INSERT INTO po_bostedsadresse (id,
                               type,
                               fom,
                               tom,
                               fk_po_person_id,
                               opprettet_av,
                               opprettet_tid,
                               endret_av,
                               endret_tid,
                               versjon)
    (SELECT NEXTVAL('po_bostedsadresse_seq'),
            'Ikke satt', -- TODO: Må settes til en verdi som ikke skaper problemer ved lesing fra database
            periode.fom,
            periode.tom,
            periode.fk_po_person_id,
            periode.opprettet_av,
            periode.opprettet_tid,
            periode.endret_av,
            periode.endret_tid,
            periode.versjon
     FROM po_bostedsadresseperiode periode);

ALTER TABLE po_person DROP COLUMN sivilstand;
ALTER TABLE po_person DROP COLUMN bostedsadresse_id;
DROP TABLE po_bostedsadresseperiode;