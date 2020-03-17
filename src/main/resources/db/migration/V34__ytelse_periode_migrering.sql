INSERT INTO YTELSE_PERIODE (fk_vedtak_person_id, belop, stonad_fom, stonad_tom, type)
SELECT id, belop, stonad_fom, stonad_tom, type FROM VEDTAK_PERSON;