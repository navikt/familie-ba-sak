CREATE TABLE IF NOT EXISTS endring_i_preutfylt_vilkar_logg
(
    id                     BIGINT  NOT NULL PRIMARY KEY,
    vilkar_type            VARCHAR NOT NULL,
    fk_behandling_id       BIGINT REFERENCES behandling (id) NOT NULL,
    begrunnelse            VARCHAR,
    forrige_fom            TIMESTAMP,
    ny_fom                 TIMESTAMP,
    forrige_tom            TIMESTAMP,
    ny_tom                 TIMESTAMP,
    forrige_resultat       VARCHAR,
    ny_resultat            VARCHAR,
    forrige_vurderes_etter VARCHAR,
    ny_vurderes_etter      VARCHAR,
    forrige_utdypende_vilkårsvurdering VARCHAR,
    ny_utdypende_vilkårsvurdering VARCHAR
);

CREATE SEQUENCE IF NOT EXISTS endring_i_preutfylt_vilkar_logg_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;