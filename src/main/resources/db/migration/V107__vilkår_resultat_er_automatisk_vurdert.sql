ALTER TABLE VILKAR_RESULTAT
    ADD COLUMN er_automatisk_vurdert BOOLEAN DEFAULT FALSE NOT NULL;


UPDATE VILKAR_RESULTAT
SET er_automatisk_vurdert = TRUE
WHERE vilkar = 'UNDER_18_ÅR';