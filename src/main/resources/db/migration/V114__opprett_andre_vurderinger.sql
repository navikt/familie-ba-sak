CREATE TABLE ANDRE_VURDERINGER
(
    ID                    BIGINT PRIMARY KEY,
    FK_BEHANDLING_ID      BIGINT REFERENCES behandling (id)      NOT NULL,
    FK_PERSON_RESULTAT_ID BIGINT REFERENCES person_resultat (id) NOT NULL,
    RESULTAT              VARCHAR                                NOT NULL,
    TYPE                  VARCHAR                                NOT NULL,
    BEGRUNNELSE           TEXT,
    VERSJON               BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV          VARCHAR      DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID         TIMESTAMP(3) DEFAULT localtimestamp    NOT NULL,
    ENDRET_AV             VARCHAR,
    ENDRET_TID            TIMESTAMP(3)
);

CREATE SEQUENCE ANDRE_VURDERINGER_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
