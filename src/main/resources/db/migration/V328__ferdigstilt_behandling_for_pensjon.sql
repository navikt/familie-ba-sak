CREATE TABLE ferdigstilt_behandling_for_pensjon
(
    sekvensnummer     BIGINT PRIMARY KEY CHECK (sekvensnummer > 0),
    behandling_id     BIGINT      NOT NULL UNIQUE,
    ident             VARCHAR(11) NOT NULL,
    vedtaktidspunkt   TIMESTAMP   NOT NULL,
    endringstidspunkt DATE        NOT NULL
);
