ALTER TABLE ANDEL_TILKJENT_YTELSE
    ADD COLUMN tilkjent_ytelse_id bigint references tilkjent_ytelse(id);