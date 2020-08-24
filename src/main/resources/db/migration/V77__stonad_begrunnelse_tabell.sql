CREATE TABLE STONAD_BREV_BEGRUNNELSE
(
    id                  bigint primary key,
    fk_vedtak_id        bigint references vedtak(id),
    fom                 TIMESTAMP(3),
    tom                 TIMESTAMP(3),
    begrunnelse         VARCHAR,
    arsak               VARCHAR
);

CREATE SEQUENCE STONAD_BREV_BEGRUNNELSE_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;

ALTER TABLE STONAD_BREV_BEGRUNNELSE
ADD CONSTRAINT FK_VEDTAK_ID_STONAD_BREV_BEGRUNNELSE_ID FOREIGN KEY (FK_VEDTAK_ID) REFERENCES VEDTAK (ID);

