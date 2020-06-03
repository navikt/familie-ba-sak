ALTER TABLE ANDEL_TILKJENT_YTELSE
    ADD COLUMN person_ident varchar;

UPDATE ANDEL_TILKJENT_YTELSE
SET PERSON_IDENT = (
    SELECT P.person_ident
    FROM po_person P
             JOIN andel_tilkjent_ytelse aty ON p.id = aty.fk_person_id);

ALTER TABLE ANDEL_TILKJENT_YTELSE
    ALTER COLUMN person_ident SET NOT NULL;
