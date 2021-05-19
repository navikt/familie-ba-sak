ALTER TABLE po_bostedsadresse
    ADD COLUMN fk_po_person_id BIGINT REFERENCES po_person (id);