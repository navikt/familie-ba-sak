CREATE TABLE IF NOT EXISTS task (
    id BIGINT NOT NULL CONSTRAINT henvendelse_pkey PRIMARY KEY,
    payload TEXT NOT NULL,
    status VARCHAR(15)  DEFAULT 'UBEHANDLET':: CHARACTER VARYING NOT NULL,
    versjon BIGINT DEFAULT 0,
    opprettet_tid TIMESTAMP(3) DEFAULT LOCALTIMESTAMP,
    type          VARCHAR(100) NOT NULL,
    metadata      VARCHAR(4000),
    trigger_tid   TIMESTAMP DEFAULT LOCALTIMESTAMP,
    avvikstype    VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS henvendelse_status_idx ON task (status);

CREATE TABLE IF NOT EXISTS task_logg (
    id BIGINT NOT NULL CONSTRAINT henvendelse_logg_pkey PRIMARY KEY,
    task_id BIGINT NOT NULL CONSTRAINT henvendelse_logg_henvendelse_id_fkey
            REFERENCES task,
    type VARCHAR(15)  NOT NULL,
    node          VARCHAR(100) NOT NULL,
    opprettet_tid TIMESTAMP(3) DEFAULT LOCALTIMESTAMP,
    melding       TEXT,
    endret_av     VARCHAR(100) DEFAULT 'VL':: CHARACTER VARYING
);

CREATE INDEX IF NOT EXISTS henvendelse_logg_henvendelse_id_idx
    ON task_logg (task_id);

CREATE SEQUENCE IF NOT EXISTS task_seq INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS task_logg_seq INCREMENT BY 50;
