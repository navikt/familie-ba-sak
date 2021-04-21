CREATE TABLE tilbakekreving (
    id              BIGINT PRIMARY KEY,
    fk_vedtak_id    BIGINT REFERENCES vedtak (id),

    valg VARCHAR(50) NOT NULL,
    varsel   text,
    begrunnelse   text NOT NULL,
    tilbakekrevingsbehandling_id   BIGINT,

    opprettet_av VARCHAR(512) DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av VARCHAR(512),
    endret_tid TIMESTAMP(3),
    versjon BIGINT DEFAULT 0
);

CREATE SEQUENCE tilbakekreving_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE INDEX ON tilbakekreving (fk_vedtak_id);