ALTER TABLE vilkar_resultat ADD COLUMN PERIODE_FOM TIMESTAMP(3) DEFAULT NULL;
ALTER TABLE vilkar_resultat ADD COLUMN PERIODE_TOM TIMESTAMP(3) DEFAULT NULL;
ALTER TABLE periode_resultat DROP COLUMN PERIODE_FOM;
ALTER TABLE periode_resultat DROP COLUMN PERIODE_TOM;
ALTER TABLE periode_resultat rename TO person_resultat;