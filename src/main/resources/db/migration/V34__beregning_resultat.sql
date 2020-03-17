CREATE TABLE BEREGNINGRESULTAT (
    ID bigint primary key,
    FK_BEHANDLING_ID bigint references BEHANDLING (id),
    STONAD_FOM timestamp not null,
    STONAD_TOM timestamp not null,
    OPPRETTET_DATO timestamp not null,
    ER_OPPHOER boolean not null default false,
    UTBETALINGSOPPDRAG json not null
);