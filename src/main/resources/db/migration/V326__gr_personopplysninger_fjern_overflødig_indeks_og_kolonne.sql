DROP INDEX uidx_gr_personopplysninger_01;
DROP INDEX gr_personopplysninger_fk_behandling_id_idx;

ALTER TABLE gr_personopplysninger
    DROP COLUMN aktiv;
