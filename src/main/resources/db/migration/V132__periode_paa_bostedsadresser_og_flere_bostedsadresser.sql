ALTER TABLE po_bostedsadresse
    ADD COLUMN fom DATE;
ALTER TABLE po_bostedsadresse
    ADD COLUMN tom DATE;

ALTER TABLE po_bostedsadresse
    ADD COLUMN fk_po_person_id BIGINT REFERENCES po_person (id);