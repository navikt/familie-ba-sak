CREATE TABLE tilbakekreving (
    id              BIGINT PRIMARY KEY,
    fk_vedtak_id    BIGINT REFERENCES vedtak (id),

    type VARCHAR(50),
    varsel   text,
    beskrivelse   text,

    opprettet_av VARCHAR(512) DEFAULT 'VL'::CHARACTER VARYING NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL,
    endret_av VARCHAR(512),
    endret_tid TIMESTAMP(3),
    versjon BIGINT DEFAULT 0
);

CREATE SEQUENCE tilbakekreving_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE INDEX ON tilbakekreving (fk_vedtak_id);