-- Det ser ut som at deploy til dev henger når man prøver å
-- migrere data. Derfor kommenterer jeg ut denne delen.
/*INSERT INTO
    BEHANDLING_STEG_TILSTAND(fk_behandling_id, behandling_steg)
    (
        SELECT
            id, steg
        FROM
            BEHANDLING
        WHERE
            steg is not null
     );*/
