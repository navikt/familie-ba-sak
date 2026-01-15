### Spørring for å hente ut fagsaker med utbetaling fra annet land inneværende måned

1. Henter siste vedtatte behandling per løpende fagsak.
2. Filtrerer behandlinger som har utbetaling fra et annet land i inneværende måned, der Norge er sekundærland.
3. Legger på total utbetaling inneværende måned fra Norge.
4. Legger på behandlende enhet.

For å gjøre uttrekk for et land, bytt ut `<SETT INN LANDKODE HER>` med landkode for ønsket land (f.eks. 'SE' for Sverige).

```postgresql
WITH siste_vedtatte_behandling_per_fagsak AS (
    SELECT DISTINCT ON (b.fk_fagsak_id) b.id, b.fk_fagsak_id
    FROM behandling b
        JOIN fagsak f ON f.id = b.fk_fagsak_id
    WHERE f.status = 'LØPENDE'
      AND f.arkivert = false
      AND b.status = 'AVSLUTTET'
      AND b.resultat NOT LIKE '%HENLAGT%'
    ORDER BY b.fk_fagsak_id, b.aktivert_tid DESC
),
behandlinger_med_utbetaling_fra_annet_land_inneværende_måned AS (
    SELECT DISTINCT ON (b.id) b.*
    FROM siste_vedtatte_behandling_per_fagsak b
        JOIN kompetanse k on k.fk_behandling_id = b.id
        JOIN utenlandsk_periodebeloep upb on upb.fk_behandling_id = b.id
    WHERE (k.tom IS NULL OR k.tom >= DATE_TRUNC('MONTH', CURRENT_DATE))
      AND (upb.tom IS NULL OR upb.tom >= DATE_TRUNC('MONTH', CURRENT_DATE))
      AND k.resultat = 'NORGE_ER_SEKUNDÆRLAND'
      AND upb.utbetalingsland = '<SETT INN LANDKODE HER>'
),
behandlinger_med_total_utbetaling AS (
    SELECT b.*, SUM(aty.kalkulert_utbetalingsbelop) AS total_utbetaling
    FROM behandlinger_med_utbetaling_fra_annet_land_inneværende_måned b
        JOIN andel_tilkjent_ytelse aty on aty.fk_behandling_id = b.id
    WHERE aty.stonad_fom <= DATE_TRUNC('MONTH', CURRENT_DATE)
      AND aty.stonad_tom >= DATE_TRUNC('MONTH', CURRENT_DATE)
    GROUP BY b.id, b.fk_fagsak_id
),
behandlinger_med_behandlende_enhet AS (
    SELECT b.*, apb.behandlende_enhet_id
    FROM behandlinger_med_total_utbetaling b
        JOIN arbeidsfordeling_pa_behandling apb ON b.id = apb.fk_behandling_id
)
SELECT b.fk_fagsak_id, b.total_utbetaling, b.behandlende_enhet_id
FROM behandlinger_med_behandlende_enhet b;
```