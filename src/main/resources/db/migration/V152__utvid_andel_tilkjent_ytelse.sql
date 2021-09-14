ALTER TABLE andel_tilkjent_ytelse
    ADD COLUMN endring_typer TEXT DEFAULT '';

ALTER TABLE andel_tilkjent_ytelse
    ADD COLUMN prosent NUMERIC;
UPDATE andel_tilkjent_ytelse
SET prosent = 100;
ALTER TABLE andel_tilkjent_ytelse
    ALTER COLUMN prosent SET NOT NULL;

ALTER TABLE andel_tilkjent_ytelse
    ADD COLUMN sats BIGINT;
UPDATE andel_tilkjent_ytelse
SET sats = andel_tilkjent_ytelse.belop;
ALTER TABLE andel_tilkjent_ytelse
    ALTER COLUMN sats SET NOT NULL;


