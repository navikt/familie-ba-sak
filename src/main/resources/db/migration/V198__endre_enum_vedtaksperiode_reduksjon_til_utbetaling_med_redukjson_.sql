UPDATE "familie-ba-sak".public.vedtaksperiode
SET type='UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING'
WHERE type = 'REDUKSJON';
