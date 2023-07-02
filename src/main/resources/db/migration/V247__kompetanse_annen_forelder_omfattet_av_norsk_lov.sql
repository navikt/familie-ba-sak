ALTER TABLE kompetanse
    ADD COLUMN IF NOT EXISTS annen_forelder_omfattet_av_norsk_lovgivning boolean default false;
