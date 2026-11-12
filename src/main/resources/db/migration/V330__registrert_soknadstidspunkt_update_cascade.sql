ALTER TABLE registrert_soknadstidspunkt_paa_person
    DROP CONSTRAINT registrert_soknadstidspunkt_paa_person_fk_aktoer_id_fkey,
    ADD CONSTRAINT registrert_soknadstidspunkt_paa_person_fk_aktoer_id_fkey
        FOREIGN KEY (fk_aktoer_id) REFERENCES aktoer (aktoer_id) ON UPDATE CASCADE;
