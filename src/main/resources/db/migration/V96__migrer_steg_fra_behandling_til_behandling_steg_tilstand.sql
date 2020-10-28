INSERT INTO
    BEHANDLING_STEG_TILSTAND(fk_behandling_id, behandling_steg)
    (
        SELECT
            id, steg
        FROM
            BEHANDLING
        WHERE
            steg is not null
     );
