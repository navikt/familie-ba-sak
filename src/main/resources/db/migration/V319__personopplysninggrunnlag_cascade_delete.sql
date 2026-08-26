SET LOCAL lock_timeout = '15s';

ALTER TABLE po_person
    DROP CONSTRAINT po_person_fk_gr_personopplysninger_id_fkey,
    ADD CONSTRAINT po_person_fk_gr_personopplysninger_id_fkey
        FOREIGN KEY (fk_gr_personopplysninger_id)
            REFERENCES gr_personopplysninger (id)
            ON DELETE CASCADE NOT VALID;

ALTER TABLE po_statsborgerskap
    DROP CONSTRAINT po_statsborgerskap_fk_po_person_id_fkey,
    ADD CONSTRAINT po_statsborgerskap_fk_po_person_id_fkey
        FOREIGN KEY (fk_po_person_id)
            REFERENCES po_person (id)
            ON DELETE CASCADE NOT VALID;

ALTER TABLE po_opphold
    DROP CONSTRAINT po_opphold_fk_po_person_id_fkey,
    ADD CONSTRAINT po_opphold_fk_po_person_id_fkey
        FOREIGN KEY (fk_po_person_id)
            REFERENCES po_person (id)
            ON DELETE CASCADE NOT VALID;

ALTER TABLE po_arbeidsforhold
    DROP CONSTRAINT po_arbeidsforhold_fk_po_person_id_fkey,
    ADD CONSTRAINT po_arbeidsforhold_fk_po_person_id_fkey
        FOREIGN KEY (fk_po_person_id)
            REFERENCES po_person (id)
            ON DELETE CASCADE NOT VALID;

ALTER TABLE po_sivilstand
    DROP CONSTRAINT po_sivilstand_fk_po_person_id_fkey,
    ADD CONSTRAINT po_sivilstand_fk_po_person_id_fkey
        FOREIGN KEY (fk_po_person_id)
            REFERENCES po_person (id)
            ON DELETE CASCADE NOT VALID;

ALTER TABLE po_bostedsadresse
    DROP CONSTRAINT po_bostedsadresse_fk_po_person_id_fkey,
    ADD CONSTRAINT po_bostedsadresse_fk_po_person_id_fkey
        FOREIGN KEY (fk_po_person_id)
            REFERENCES po_person (id)
            ON DELETE CASCADE NOT VALID;

ALTER TABLE po_doedsfall
    DROP CONSTRAINT po_doedsfall_fk_po_person_id_fkey,
    ADD CONSTRAINT po_doedsfall_fk_po_person_id_fkey
        FOREIGN KEY (fk_po_person_id)
            REFERENCES po_person (id)
            ON DELETE CASCADE NOT VALID;
