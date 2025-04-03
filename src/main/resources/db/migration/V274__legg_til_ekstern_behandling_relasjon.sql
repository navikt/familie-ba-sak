CREATE TABLE IF NOT EXISTS EKSTERN_BEHANDLING_RELASJON
(
    id                                  BIGINT PRIMARY KEY,
    intern_behandling_id                BIGINT REFERENCES BEHANDLING (ID)           NOT NULL,
    ekstern_behandling_id               VARCHAR(512)                                NOT NULL,
    ekstern_behandling_fagsystem        VARCHAR(512)                                NOT NULL,
    opprettet_tidspunkt                 TIMESTAMP(3)                                NOT NULL
);

CREATE SEQUENCE ekstern_behandling_relasjon_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE INDEX intern_behandling_id_idx ON EKSTERN_BEHANDLING_RELASJON (intern_behandling_id);