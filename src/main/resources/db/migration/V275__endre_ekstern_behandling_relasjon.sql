ALTER TABLE IF EXISTS EKSTERN_BEHANDLING_RELASJON RENAME COLUMN intern_behandling_id TO fk_behandling_id;
ALTER TABLE IF EXISTS EKSTERN_BEHANDLING_RELASJON RENAME COLUMN opprettet_tidspunkt TO opprettet_tid;

ALTER INDEX IF EXISTS intern_behandling_id_idx RENAME TO ekstern_behandling_relasjon_fk_behandling_id_idx;

ALTER TABLE IF EXISTS EKSTERN_BEHANDLING_RELASJON ALTER COLUMN ekstern_behandling_id TYPE VARCHAR;
ALTER TABLE IF EXISTS EKSTERN_BEHANDLING_RELASJON ALTER COLUMN ekstern_behandling_fagsystem TYPE VARCHAR;

ALTER TABLE IF EXISTS EKSTERN_BEHANDLING_RELASJON ADD CONSTRAINT ekstern_behandling_relasjon_til_fk_behandling_id_fkey FOREIGN KEY (fk_behandling_id) REFERENCES behandling (id) ON DELETE CASCADE;
ALTER TABLE IF EXISTS EKSTERN_BEHANDLING_RELASJON ADD CONSTRAINT unik_ekstern_behandling_relasjon UNIQUE (fk_behandling_id, ekstern_behandling_fagsystem);