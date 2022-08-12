Skriptet som ligg her er ei samanslåing av alle skripta frå main/resources/db/migration, per skrivande stund t.o.m. 219.

Samanslåinga er gjort ved å
1. Starte databasen i ein postgres-container etter oppskrifta i readme
1. Starte applikasjonen og dermed få køyrd flyway-migreringa med desse
1. Køyre følgjande kommando: pg_dump --host="localhost" --port="5432" --username="postgres" --no-owner --exclude-table=flyway_schema_history* "familie-ba-sak" > ut.sql
1. Døpe om ut.sql og leggje i test/resources/db/migration  