ALTER TABLE vilkar_resultat
    ADD COLUMN er_opprinnelig_preutfylt_i_behandling BIGINT REFERENCES behandling (id);

UPDATE vilkar_resultat
SET er_opprinnelig_preutfylt_i_behandling = sist_endret_i_behandling_id
WHERE er_opprinnelig_preutfylt = true;

ALTER TABLE vilkar_resultat
    DROP COLUMN er_opprinnelig_preutfylt;