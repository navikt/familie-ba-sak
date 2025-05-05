ALTER TABLE tilbakekrevingsvedtak_motregning
    ADD COLUMN hele_belopet_skal_kreves_tilbake BOOLEAN DEFAULT FALSE NOT NULL;