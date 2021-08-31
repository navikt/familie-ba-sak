CREATE TABLE OVERSTYRT_UTBETALING
(
    ID                  BIGINT PRIMARY KEY,
    FK_VEDTAK_ID        BIGINT REFERENCES VEDTAK (ID)       NOT NULL,
    FK_PO_PERSON_ID     BIGINT REFERENCES PO_PERSON (ID)    NOT NULL,
    FOM                 TIMESTAMP(3)                        NOT NULL,
    TOM                 TIMESTAMP(3)                        NOT NULL,
    PROSENT             NUMERIC                             NOT NULL,
    AARSAK              VARCHAR                             NOT NULL,
    BEGRUNNELSE         TEXT                                NOT NULL,
    VERSJON             BIGINT       DEFAULT 0              NOT NULL,
    OPPRETTET_AV        VARCHAR      DEFAULT 'VL'           NOT NULL,
    OPPRETTET_TID       TIMESTAMP(3) DEFAULT localtimestamp NOT NULL,
    ENDRET_AV           VARCHAR,
    ENDRET_TID          TIMESTAMP(3)
);

CREATE SEQUENCE OVERSTYRT_UTBETALING_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
CREATE INDEX ON OVERSTYRT_UTBETALING (FK_VEDTAK_ID);

