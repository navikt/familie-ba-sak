create TABLE po_doedsfall
(
    id                      bigint                                      PRIMARY KEY,
    fk_po_person_id         bigint       REFERENCES PO-PERSON (id)      NOT NULL,
    versjon                 bigint       DEFAULT 0                      NOT NULL,
    doedsfall_dato          TIMESTAMP(3)                                NOT NULL,
    adresse                 VARCHAR      DEFAULT null,
    postnummer              VARCHAR      DEFAULT null,
    poststed                VARCHAR      DEFAULT null
);

create sequence po_doedsfall_seq increment by 50 start with 1000000 NO CYCLE;

create INDEX po_doedsfall_fk_po_person_id_idx ON po_doedsfall(fk_po_person_id);



