ALTER TABLE behandling ADD COLUMN aktivert_tid TIMESTAMP(3); -- NOT NULL + oppdatere burde skje i egen deploy

-- Kun en behandling kan ha status annet enn AVSLUTTET eller SATT_PÅ_VENT
CREATE UNIQUE INDEX UIDX_BEHANDLING_02 ON behandling (fk_fagsak_id)
    WHERE (status <> 'AVSLUTTET' AND status <> 'SATT_PÅ_VENT');
-- Kun en behandling kan ha status SATT_PÅ_VENT
CREATE UNIQUE INDEX UIDX_BEHANDLING_03 ON behandling (fk_fagsak_id)
    WHERE status = 'SATT_PÅ_VENT';

DROP INDEX UIDX_BEHANDLING_01;