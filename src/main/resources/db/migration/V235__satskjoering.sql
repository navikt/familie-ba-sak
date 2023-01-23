CREATE TABLE satskjoering
(
    ID           BIGINT                              NOT NULL PRIMARY KEY,
    FK_FAGSAK_ID BIGINT                              NOT NULL,
    START_TID    TIMESTAMP(3) DEFAULT LOCALTIMESTAMP NOT NULL,
    FERDIG_TID   TIMESTAMP(3)
);

CREATE SEQUENCE SATSKJOERING_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE INDEX satskjoering_fagsak_id_idx ON satskjoering (fk_fagsak_id);