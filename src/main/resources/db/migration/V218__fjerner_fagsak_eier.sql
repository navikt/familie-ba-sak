DROP INDEX uidx_fagsak_eier_aktoer_ikke_arkivert;

ALTER TABLE fagsak
    DROP COLUMN eier;
