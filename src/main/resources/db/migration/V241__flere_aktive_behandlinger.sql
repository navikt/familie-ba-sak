UPDATE behandling
SET status = 'SATT_PÅ_VENT'
WHERE id IN (SELECT fk_behandling_id FROM sett_paa_vent WHERE aktiv = true);

CREATE UNIQUE INDEX UIDX_BEHANDLING_02 ON behandling (fk_fagsak_id)
    WHERE (status <> 'AVSLUTTET' AND status <> 'SATT_PÅ_VENT');

DROP INDEX UIDX_BEHANDLING_01;