DO $$
    BEGIN
        IF EXISTS
            ( SELECT 1 from pg_roles where rolname='cloudsqliamuser')
        THEN
            ${ignoreIfProd} GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO cloudsqliamuser;
        END IF ;
    END
$$ ;