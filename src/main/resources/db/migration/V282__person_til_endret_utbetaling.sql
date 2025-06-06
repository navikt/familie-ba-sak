-- Lag en ny koblingstabell for personer og endret utbetaling andeler
CREATE TABLE PERSON_TIL_ENDRET_UTBETALING_ANDEL
(
    fk_endret_utbetaling_andel_id BIGINT NOT NULL,
    fk_person_id                  BIGINT NOT NULL,

    PRIMARY KEY (fk_endret_utbetaling_andel_id, fk_person_id),
    CONSTRAINT fk_endret_utbetaling_andel FOREIGN KEY (fk_endret_utbetaling_andel_id)
        REFERENCES ENDRET_UTBETALING_ANDEL (id) ON DELETE CASCADE,
    CONSTRAINT fk_po_person FOREIGN KEY (fk_person_id)
        REFERENCES PO_PERSON (id) ON UPDATE CASCADE
);

-- Migrere data fra ENDRET_UTBETALING_ANDEL til den nye tabellen
INSERT INTO PERSON_TIL_ENDRET_UTBETALING_ANDEL (fk_endret_utbetaling_andel_id, fk_person_id)
SELECT id AS endret_utbetaling_andel_id, fk_po_person_id AS person_id
FROM ENDRET_UTBETALING_ANDEL
WHERE fk_po_person_id IS NOT NULL;

-- Dropp fk_po_person_id kolonnen fra ENDRET_UTBETALING_ANDEL
ALTER TABLE ENDRET_UTBETALING_ANDEL
    DROP COLUMN fk_po_person_id;