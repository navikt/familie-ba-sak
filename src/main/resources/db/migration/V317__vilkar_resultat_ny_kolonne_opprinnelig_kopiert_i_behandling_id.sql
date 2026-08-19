-- Expand-contract: legger til et nytt felt som peker på behandlingId i stedet for
-- id-en til den konkrete VILKAR_RESULTAT-raden det ble kopiert fra (som kan slettes
-- uavhengig av kopien, f.eks. ved redigering av søkers vilkår). Det gamle feltet
-- opprinnelig_kopiert_fra_vilkar_resultat fjernes i en senere migrering når all
-- lesing/skriving er flyttet over til det nye feltet.
ALTER TABLE VILKAR_RESULTAT
    ADD COLUMN IF NOT EXISTS opprinnelig_kopiert_i_behandling_id BIGINT;
