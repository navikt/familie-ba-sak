ALTER TABLE behandling
    ADD COLUMN gjeldendeForNesteUtbetaling boolean default false not null;
