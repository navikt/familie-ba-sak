ALTER TABLE oppdater_utvidet_klassekode_kjoring
    ADD COLUMN status VARCHAR(50) DEFAULT 'IKKE_UTFØRT' NOT NULL;