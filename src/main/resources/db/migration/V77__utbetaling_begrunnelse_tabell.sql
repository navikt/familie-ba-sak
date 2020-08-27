CREATE TABLE UTBETALING_BEGRUNNELSE
(
    id                 bigint primary key NOT NULL,
    fk_vedtak_id       bigint references vedtak (id),
    fom                TIMESTAMP(3)       NOT NULL,
    tom                TIMESTAMP(3)       NOT NULL,
    resultat           VARCHAR,
    vedtak_begrunnelse VARCHAR,
    brev_begrunnelse   TEXT
);

CREATE SEQUENCE UTBETALING_BEGRUNNELSE_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;

ALTER TABLE UTBETALING_BEGRUNNELSE
    ADD CONSTRAINT FK_VEDTAK_ID_UTBETALING_BEGRUNNELSE_ID FOREIGN KEY (FK_VEDTAK_ID) REFERENCES VEDTAK (ID);

