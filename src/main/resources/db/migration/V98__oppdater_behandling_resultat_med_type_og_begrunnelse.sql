ALTER TABLE BEHANDLING_RESULTAT DROP COLUMN IF EXISTS henlegg_arsak;
ALTER TABLE BEHANDLING_RESULTAT DROP COLUMN IF EXISTS begrunnelse;

ALTER TABLE BEHANDLING_RESULTAT ADD COLUMN henlegg_arsak VARCHAR;
ALTER TABLE BEHANDLING_RESULTAT ADD COLUMN begrunnelse VARCHAR;
