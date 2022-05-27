CREATE TABLE eos_periodebegrunnelse
(
    id                   BIGINT  NOT NULL PRIMARY KEY,
    fk_vedtaksperiode_id BIGINT REFERENCES vedtaksperiode ON DELETE CASCADE,
    begrunnelse          VARCHAR NOT NULL
);

CREATE INDEX eos_periodebegrunnelse_fk_vedtaksperiode_id_idx
    ON eos_periodebegrunnelse (fk_vedtaksperiode_id);

ALTER TABLE vedtaksbegrunnelse
    RENAME TO nasjonal_periodebegrunnelse;

ALTER TABLE nasjonal_periodebegrunnelse
    RENAME COLUMN vedtak_begrunnelse_spesifikasjon TO begrunnelse;
