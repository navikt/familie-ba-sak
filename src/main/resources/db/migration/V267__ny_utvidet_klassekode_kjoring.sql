CREATE TABLE ny_utvidet_klassekode_kjoring
(
    ID                   BIGINT                                          NOT NULL PRIMARY KEY,
    FK_FAGSAK_ID         BIGINT REFERENCES FAGSAK (ID) ON DELETE CASCADE NOT NULL UNIQUE,
    BRUKER_NY_KLASSEKODE BOOLEAN DEFAULT FALSE                           NOT NULL
);

CREATE SEQUENCE ny_utvidet_klassekode_kjoring_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE INDEX ny_utvidet_klassekode_kjoring_fagsak_id_idx ON ny_utvidet_klassekode_kjoring (fk_fagsak_id);

