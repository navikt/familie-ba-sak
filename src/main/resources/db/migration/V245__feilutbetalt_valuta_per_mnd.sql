ALTER TABLE feilutbetalt_valuta
    ADD COLUMN IF NOT EXISTS er_per_maaned BOOLEAN DEFAULT FALSE NOT NULL;