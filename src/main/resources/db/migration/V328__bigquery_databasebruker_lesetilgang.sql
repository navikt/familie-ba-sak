-- Gir BigQuery-brukeren lesetilgang til alle eksisterende og fremtidige tabeller i public.
DO $$
    BEGIN
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'basakdbbruker') THEN
            GRANT SELECT ON ALL TABLES IN SCHEMA public TO "basakdbbruker";
            ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO "basakdbbruker";
        END IF;
    END
$$;