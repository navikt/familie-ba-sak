INSERT INTO
    BEHANDLING_STEG_TILSTAND(id, fk_behandling_id, behandling_steg)
    (
        SELECT
            nextval('BEHANDLING_STEG_TILSTAND_SEQ'), id, steg
        FROM
            BEHANDLING
        WHERE
            steg is not null
     );
