ALTER TABLE VEDTAK_BEGRUNNELSE ALTER COLUMN fom DROP NOT NULL;
ALTER TABLE VEDTAK_BEGRUNNELSE ADD COLUMN person_ident VARCHAR;