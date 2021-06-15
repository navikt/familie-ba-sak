ALTER TABLE vedtaksbegrunnelse
    DROP CONSTRAINT vedtaksbegrunnelse_fk_vedtaksperiode_id_fkey,
    ADD CONSTRAINT vedtaksbegrunnelse_fk_vedtaksperiode_id_fkey
        FOREIGN KEY (fk_vedtaksperiode_id)
            REFERENCES vedtaksperiode (id)
            ON DELETE CASCADE;

ALTER TABLE andel_tilkjent_ytelse
    DROP CONSTRAINT andel_tilkjent_ytelse_tilkjent_ytelse_id_fkey,
    ADD CONSTRAINT andel_tilkjent_ytelse_tilkjent_ytelse_id_fkey
        FOREIGN KEY (tilkjent_ytelse_id)
            REFERENCES tilkjent_ytelse (id)
            ON DELETE CASCADE;

