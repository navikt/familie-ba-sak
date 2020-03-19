INSERT INTO YTELSE_PERIODE (id, fk_vedtak_person_id, belop, stonad_fom, stonad_tom, type, endret_av, endret_tid)
SELECT id, id, belop, stonad_fom, stonad_tom, type, endret_av, endret_tid FROM VEDTAK_PERSON;

select setval('YTELSE_PERIODE_SEQ', (SELECT MAX (id) FROM YTELSE_PERIODE)+1);