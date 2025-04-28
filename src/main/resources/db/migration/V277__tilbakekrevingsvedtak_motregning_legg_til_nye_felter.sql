ALTER TABLE tilbakekrevingsvedtak_motregning
DROP COLUMN fritekst,
    ADD COLUMN årsak_til_feilutbetaling VARCHAR,
    ADD COLUMN vurdering_av_skyld VARCHAR,
    ADD COLUMN varsel_dato TIMESTAMP(3);