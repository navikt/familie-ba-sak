ALTER TABLE vilkar_resultat
    ADD COLUMN utdypende_vilkarsvurderinger VARCHAR;

UPDATE vilkar_resultat
SET utdypende_vilkarsvurderinger = 'VURDERING_ANNET_GRUNNLAG'
WHERE vilkar_resultat.er_skjonnsmessig_vurdert = true
  AND vilkar_resultat.utdypende_vilkarsvurderinger IS NULL;


UPDATE vilkar_resultat
SET utdypende_vilkarsvurderinger =
        CASE
            WHEN octet_length(vilkar_resultat.utdypende_vilkarsvurderinger) > 0
                THEN concat(utdypende_vilkarsvurderinger, ';', 'VURDERT_MEDLEMSKAP')
            ELSE 'VURDERT_MEDLEMSKAP'
            END
WHERE vilkar_resultat.er_medlemskap_vurdert = true
  AND vilkar_resultat.utdypende_vilkarsvurderinger IS NULL;

UPDATE vilkar_resultat
SET utdypende_vilkarsvurderinger =
        CASE
            WHEN octet_length(vilkar_resultat.utdypende_vilkarsvurderinger) > 0
                THEN concat(utdypende_vilkarsvurderinger, ';', 'DELT_BOSTED')
            ELSE 'DELT_BOSTED'
            END
WHERE vilkar_resultat.er_delt_bosted = true
  AND vilkar_resultat.utdypende_vilkarsvurderinger IS NULL;
