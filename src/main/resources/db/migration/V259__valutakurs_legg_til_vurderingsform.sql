ALTER TABLE valutakurs
    ADD COLUMN vurderingsform TEXT DEFAULT null;

UPDATE valutakurs
SET vurderingsform = 'MANUELL';