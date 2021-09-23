ALTER TABLE fagsak_person
    ADD COLUMN arkivert     BOOLEAN      DEFAULT FALSE       NOT NULL;

ALTER TABLE fagsak
    ADD COLUMN arkivert     BOOLEAN      DEFAULT FALSE       NOT NULL;

CREATE UNIQUE INDEX uidx_fagsak_person_ident_ikke_arkivert ON fagsak_person(ident)
    WHERE arkivert = false;