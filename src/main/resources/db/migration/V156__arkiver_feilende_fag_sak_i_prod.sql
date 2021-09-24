CREATE UNIQUE INDEX uidx_fagsak_person_ident_ikke_arkivert ON fagsak_person(ident)
    WHERE arkivert = false;

UPDATE fagsak_person
SET arkivert = TRUE
WHERE fk_fagsak_id = 1078652;

UPDATE fagsak
SET arkivert = TRUE
WHERE id = 1078652;