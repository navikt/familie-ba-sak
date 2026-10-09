SET LOCAL lock_timeout = '5s';

CREATE UNIQUE INDEX uidx_gr_personopplysninger_fk_behandling_id
    ON gr_personopplysninger (fk_behandling_id);

DROP INDEX uidx_gr_personopplysninger_01;
DROP INDEX gr_personopplysninger_fk_behandling_id_idx;

ALTER TABLE gr_personopplysninger
DROP COLUMN aktiv;
