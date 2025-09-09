CREATE SEQUENCE IF NOT EXISTS institusjon_info_seq INCREMENT BY 50 START WITH 1000000 NO CYCLE;

CREATE TABLE institusjon_info
(
    id                BIGINT PRIMARY KEY DEFAULT nextval('institusjon_info_seq'),
    fk_institusjon_id BIGINT                                    NOT NULL,
    fk_behandling_id  BIGINT                                    NOT NULL,
    type              VARCHAR                                   NOT NULL,
    navn              VARCHAR                                   NOT NULL,
    adresselinje1     VARCHAR,
    adresselinje2     VARCHAR,
    adresselinje3     VARCHAR,
    postnummer        VARCHAR                                   NOT NULL,
    poststed          VARCHAR                                   NOT NULL,
    kommunenummer     VARCHAR,
    fom               DATE                                      NOT NULL,
    tom               DATE,
    versjon           BIGINT             default 0              not null,
    opprettet_av      VARCHAR            default 'VL'           not null,
    opprettet_tid     TIMESTAMP(3)       default localtimestamp not null,
    endret_av         VARCHAR,
    endret_tid        TIMESTAMP(3),
    CONSTRAINT fk_institusjon FOREIGN KEY (fk_institusjon_id) REFERENCES institusjon (id),
    CONSTRAINT fk_behandling FOREIGN KEY (fk_behandling_id) REFERENCES behandling (id)
);