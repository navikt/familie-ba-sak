# language: no
# encoding: UTF-8

Egenskap: Utbetalingsoppdrag: Opphør/simulering etter OppdaterUtvidetKlassekode-behandling

  Scenario: Opphør alle kjeder

    Gitt følgende feature toggles
      | BehandlingId | FeatureToggleId                                                | Er togglet på |
      | 1            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Nei           |
      | 2            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Ja            |
      | 3            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Ja            |


    Gitt følgende tilkjente ytelser
      | BehandlingId | Uten andeler | Fra dato | Til dato | Beløp | Ytelse             | Behandlingsårsak            | Behandlingstype |
      | 1            |              | 07.2023  | 05.2034  | 700   | UTVIDET_BARNETRYGD |                             |                 |
      | 2            |              | 07.2023  | 05.2034  | 700   | UTVIDET_BARNETRYGD | OPPDATER_UTVIDET_KLASSEKODE | REVURDERING     |
      | 3            | Ja           |          |          | 700   | UTVIDET_BARNETRYGD |                             | REVURDERING     |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Ytelse                    |
      | 1            | 07.2023  | 05.2034  |             | 700   | NY           | Nei        | 0          |                    | UTVIDET_BARNETRYGD_GAMMEL |
      | 2            | 01.2025  | 05.2034  |             | 700   | ENDR         | Nei        | 1          | 0                  | UTVIDET_BARNETRYGD        |
      | 3            | 01.2025  | 05.2034  | 07.2023     | 700   | ENDR         | Ja         | 1          | 0                  | UTVIDET_BARNETRYGD        |