ALTER TABLE PO_BOSTEDSADRESSE ADD COLUMN matrikkel_id bigint;
ALTER TABLE PO_BOSTEDSADRESSE ALTER COLUMN bostedskommune TYPE varchar;
ALTER TABLE PO_BOSTEDSADRESSE ALTER COLUMN husnummer TYPE varchar;
ALTER TABLE PO_BOSTEDSADRESSE ALTER COLUMN husbokstav TYPE varchar;
ALTER TABLE PO_BOSTEDSADRESSE ALTER COLUMN bruksenhetsnummer TYPE varchar;
ALTER TABLE PO_BOSTEDSADRESSE ALTER COLUMN adressenavn TYPE varchar;
ALTER TABLE PO_BOSTEDSADRESSE ALTER COLUMN kommunenummer TYPE varchar;
ALTER TABLE PO_BOSTEDSADRESSE ALTER COLUMN tilleggsnavn TYPE varchar;
ALTER TABLE PO_BOSTEDSADRESSE ALTER COLUMN postnummer TYPE varchar;
ALTER TABLE PO_BOSTEDSADRESSE ALTER COLUMN opprettet_av TYPE varchar;