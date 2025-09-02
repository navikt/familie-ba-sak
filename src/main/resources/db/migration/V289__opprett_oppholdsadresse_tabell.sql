CREATE TABLE IF NOT EXISTS PO_OPPHOLDSADRESSE
(
    id                 BIGINT                                 NOT NULL PRIMARY KEY,
    type               VARCHAR                                NOT NULL,
    fom                DATE,
    tom                DATE,
    fk_po_person_id    BIGINT REFERENCES PO_PERSON (id) ON DELETE CASCADE,
    opphold_annet_sted VARCHAR,

    matrikkel_id       BIGINT,
    husnummer          VARCHAR,
    husbokstav         VARCHAR,
    bruksenhetsnummer  VARCHAR,
    adressenavn        VARCHAR,
    kommunenummer      VARCHAR,
    tilleggsnavn       VARCHAR,
    postnummer         VARCHAR,

    postboks           VARCHAR,
    by_sted            VARCHAR,
    region             VARCHAR,
    landkode           VARCHAR,

    opprettet_av       VARCHAR      DEFAULT 'VL'              NOT NULL,
    opprettet_tid      TIMESTAMP(3) DEFAULT current_timestamp NOT NULL,
    endret_av          VARCHAR,
    endret_tid         TIMESTAMP(3),
    versjon            BIGINT       DEFAULT 0                 NOT NULL
);

CREATE SEQUENCE po_oppholdsadresse_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;

CREATE INDEX po_oppholdsadresse_person_id_idx ON PO_OPPHOLDSADRESSE (fk_po_person_id);