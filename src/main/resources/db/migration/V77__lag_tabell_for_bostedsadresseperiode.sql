CREATE TABLE PO_BOSTEDSADRESSEPERIODE
(
    id                  bigint primary key,
    fk_po_person_id     bigint references PO_PERSON          NOT NULL,
    fom                 DATE,
    tom                 DATE
);

CREATE SEQUENCE PO_BOSTEDSADRESSEPERIODE_SEQ INCREMENT BY 50 START WITH 1000000 NO CYCLE;
