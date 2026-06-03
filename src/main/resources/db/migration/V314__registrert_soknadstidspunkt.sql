CREATE TABLE IF NOT EXISTS registrert_soknadstidspunkt_pa_person
(
    id               BIGINT       NOT NULL PRIMARY KEY,
    fk_behandling_id BIGINT       NOT NULL REFERENCES behandling (id),
    fk_aktoer_id     VARCHAR      NOT NULL REFERENCES aktoer (aktoer_id),
    soknadstidspunkt DATE         NOT NULL,
    versjon          BIGINT       NOT NULL DEFAULT 0,
    opprettet_av     VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid    TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_av        VARCHAR,
    endret_tid       TIMESTAMP(3)
);

CREATE SEQUENCE IF NOT EXISTS registrert_soknadstidspunkt_pa_person_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;

CREATE INDEX IF NOT EXISTS registrert_soknadstidspunkt_pa_person_behandling_id_idx ON registrert_soknadstidspunkt_pa_person (fk_behandling_id);

-- Maks ett søknadstidspunkt per person per behandling
CREATE UNIQUE INDEX IF NOT EXISTS registrert_soknadstidspunkt_pa_person_behandling_aktoer_unik_idx
    ON registrert_soknadstidspunkt_pa_person (fk_behandling_id, fk_aktoer_id);

-- Marker andeler som genereres automatisk fra søknadstidspunkt (settes kun når funksjonaliteten er påskrudd).
-- Brukes for å vise dem i lesevisning. null = ikke automatisk generert (inkludert alle eksisterende andeler).
ALTER TABLE endret_utbetaling_andel
    ADD COLUMN IF NOT EXISTS er_automatisk_generert BOOLEAN;
