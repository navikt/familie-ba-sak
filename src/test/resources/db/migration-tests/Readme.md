Skriptet som ligg her er ei samanslåing av alle skripta frå main/resources/db/migration, per skrivande stund t.o.m. 219.

Samanslåinga er gjort ved å
1. Starte databasen i ein postgres-container etter oppskrifta i readme
1. Starte applikasjonen og dermed få køyrd flyway-migreringa med desse
1. Pass på at du har postgresql-kommandoar i _path_ eller at du står i postgresql\bin-mappa
1. Køyre følgjande kommando: pg_dump --host="localhost" --port="5432" --username="postgres" --no-owner --exclude-table=flyway_schema_history* "familie-ba-sak" > ut.sql
1. Døpe om ut.sql og leggje i test/resources/db/migration-tests som erstatting for V1__create_table

For å oppdatere V2__FyllFlywaySchemaHistory, gjenta dei første tre stega, men bruk f.eks. database-verktyget i IntelliJ til å eksportere flyway_schema_history-tabellen.

Merk at du heilt fint kan leggje inn nye skript i src/db/migration på vanleg måte, men desse vil da bli køyrd som separate steg i kvar test (altså tilsvarande som alle vart før denne endringa).