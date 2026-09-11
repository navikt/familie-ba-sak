UPDATE endret_utbetaling_andel
SET avtaletidspunkt_delt_bosted = NULL
WHERE avtaletidspunkt_delt_bosted IS NOT NULL
  AND aarsak <> 'DELT_BOSTED';
