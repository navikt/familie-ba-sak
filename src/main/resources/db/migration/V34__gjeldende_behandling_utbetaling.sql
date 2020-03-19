ALTER TABLE behandling
    ADD COLUMN gjeldende_for_neste_utbetaling boolean default false not null;
