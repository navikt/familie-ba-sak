ALTER TABLE FAGSAK ADD COLUMN type VARCHAR(50) DEFAULT 'NORMAL' NOT NULL;
ALTER TABLE FAGSAK ADD COLUMN fk_institusjon_id BIGINT;
ALTER TABLE FAGSAK
    ADD FOREIGN KEY (fk_institusjon_id) REFERENCES INSTITUSJON (ID);

CREATE UNIQUE INDEX uidx_fagsak_type_aktoer_institusjon_ikke_arkivert ON fagsak(type, fk_aktoer_id, fk_institusjon_id)
    WHERE arkivert = false;
