ALTER TABLE ANDEL_TILKJENT_YTELSE
    ADD COLUMN periode_offset BIGINT;
ALTER TABLE ANDEL_TILKJENT_YTELSE
    DROP COLUMN fk_person_id;