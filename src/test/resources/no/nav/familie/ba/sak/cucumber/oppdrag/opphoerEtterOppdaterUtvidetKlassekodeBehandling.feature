# language: no
# encoding: UTF-8

Egenskap: Utbetalingsoppdrag: Opphør/simulering etter OppdaterUtvidetKlassekode-behandling

  Scenario: Opphør av utvidet barnetrygd etter behandling med årsak OPPDATER_UTVIDET_KLASSEKODE

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

    Og inneværende måned er 12.2024

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Ytelse                    |
      | 1            | 07.2023  | 05.2034  |             | 700   | NY           | Nei        | 0          |                    | UTVIDET_BARNETRYGD_GAMMEL |
      | 2            | 01.2025  | 05.2034  |             | 700   | ENDR         | Nei        | 1          | 0                  | UTVIDET_BARNETRYGD        |
      | 3            | 01.2025  | 05.2034  | 07.2023     | 700   | ENDR         | Ja         | 1          | 0                  | UTVIDET_BARNETRYGD        |


  Scenario: Endring av utvidet barnetrygd etter behandling med årsak OPPDATER_UTVIDET_KLASSEKODE

    Gitt følgende feature toggles
      | BehandlingId | FeatureToggleId                                                | Er togglet på |
      | 1            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Nei           |
      | 2            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Ja            |
      | 3            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Ja            |


    Gitt følgende tilkjente ytelser
      | BehandlingId | Uten andeler | Fra dato | Til dato | Beløp | Ytelse             | Behandlingsårsak            | Behandlingstype |
      | 1            |              | 07.2023  | 05.2034  | 700   | UTVIDET_BARNETRYGD |                             |                 |
      | 2            |              | 07.2023  | 05.2034  | 700   | UTVIDET_BARNETRYGD | OPPDATER_UTVIDET_KLASSEKODE | REVURDERING     |
      | 3            |              | 07.2023  | 05.2026  | 750   | UTVIDET_BARNETRYGD |                             | REVURDERING     |

    Og inneværende måned er 12.2024

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Ytelse                    |
      | 1            | 07.2023  | 05.2034  |             | 700   | NY           | Nei        | 0          |                    | UTVIDET_BARNETRYGD_GAMMEL |
      | 2            | 01.2025  | 05.2034  |             | 700   | ENDR         | Nei        | 1          | 0                  | UTVIDET_BARNETRYGD        |
      | 3            | 07.2023  | 05.2026  |             | 750   | ENDR         | Nei        | 2          | 1                  | UTVIDET_BARNETRYGD        |


  Scenario: Opphør av utvidet barnetrygd 2 behandlinger etter behandling med årsak OPPDATER_UTVIDET_KLASSEKODE

    Gitt følgende feature toggles
      | BehandlingId | FeatureToggleId                                                | Er togglet på |
      | 1            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Nei           |
      | 2            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Ja            |
      | 3            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Ja            |


    Gitt følgende tilkjente ytelser
      | BehandlingId | Uten andeler | Fra dato | Til dato | Beløp | Ytelse             | Behandlingsårsak            | Behandlingstype |
      | 1            |              | 07.2023  | 05.2034  | 700   | UTVIDET_BARNETRYGD |                             |                 |
      | 2            |              | 07.2023  | 05.2034  | 700   | UTVIDET_BARNETRYGD | OPPDATER_UTVIDET_KLASSEKODE | REVURDERING     |
      | 3            |              | 07.2023  | 05.2026  | 500   | ORDINÆR_BARNETRYGD |                             | REVURDERING     |
      | 3            |              | 07.2023  | 05.2034  | 700   | UTVIDET_BARNETRYGD |                             | REVURDERING     |
      | 4            |              | 07.2023  | 05.2026  | 400   | ORDINÆR_BARNETRYGD |                             | REVURDERING     |
      | 4            |              | 07.2023  | 05.2034  | 700   | UTVIDET_BARNETRYGD |                             | REVURDERING     |
      | 5            |              | 07.2023  | 05.2026  | 400   | ORDINÆR_BARNETRYGD |                             | REVURDERING     |

    Og inneværende måned er 12.2024

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Ytelse                    |
      | 1            | 07.2023  | 05.2034  |             | 700   | NY           | Nei        | 0          |                    | UTVIDET_BARNETRYGD_GAMMEL |
      | 2            | 01.2025  | 05.2034  |             | 700   | ENDR         | Nei        | 1          | 0                  | UTVIDET_BARNETRYGD        |
      | 3            | 07.2023  | 05.2026  |             | 500   | ENDR         | Nei        | 2          |                    | ORDINÆR_BARNETRYGD        |
      | 4            | 07.2023  | 05.2026  |             | 400   | ENDR         | Nei        | 3          | 2                  | ORDINÆR_BARNETRYGD        |
      | 5            | 01.2025  | 05.2034  | 07.2023     | 700   | ENDR         | Ja         | 1          | 0                  | UTVIDET_BARNETRYGD        |

  Scenario: Opphør/forkorting av utvidet barnetrygd 2 behandlinger etter behandling med årsak OPPDATER_UTVIDET_KLASSEKODE

    Gitt følgende feature toggles
      | BehandlingId | FeatureToggleId                                                | Er togglet på |
      | 1            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Nei           |
      | 2            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Ja            |
      | 3            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Ja            |


    Gitt følgende tilkjente ytelser
      | BehandlingId | Uten andeler | Fra dato | Til dato | Beløp | Ytelse             | Behandlingsårsak            | Behandlingstype |
      | 1            |              | 07.2023  | 05.2034  | 700   | UTVIDET_BARNETRYGD |                             |                 |
      | 2            |              | 07.2023  | 05.2034  | 700   | UTVIDET_BARNETRYGD | OPPDATER_UTVIDET_KLASSEKODE | REVURDERING     |
      | 3            |              | 07.2023  | 05.2026  | 500   | ORDINÆR_BARNETRYGD |                             | REVURDERING     |
      | 3            |              | 07.2023  | 05.2034  | 700   | UTVIDET_BARNETRYGD |                             | REVURDERING     |
      | 4            |              | 07.2023  | 05.2026  | 400   | ORDINÆR_BARNETRYGD |                             | REVURDERING     |
      | 4            |              | 07.2023  | 05.2034  | 700   | UTVIDET_BARNETRYGD |                             | REVURDERING     |
      | 5            |              | 07.2023  | 05.2026  | 400   | ORDINÆR_BARNETRYGD |                             | REVURDERING     |
      | 5            |              | 07.2023  | 05.2030  | 700   | UTVIDET_BARNETRYGD |                             | REVURDERING     |

    Og inneværende måned er 12.2024

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Ytelse                    |
      | 1            | 07.2023  | 05.2034  |             | 700   | NY           | Nei        | 0          |                    | UTVIDET_BARNETRYGD_GAMMEL |
      | 2            | 01.2025  | 05.2034  |             | 700   | ENDR         | Nei        | 1          | 0                  | UTVIDET_BARNETRYGD        |
      | 3            | 07.2023  | 05.2026  |             | 500   | ENDR         | Nei        | 2          |                    | ORDINÆR_BARNETRYGD        |
      | 4            | 07.2023  | 05.2026  |             | 400   | ENDR         | Nei        | 3          | 2                  | ORDINÆR_BARNETRYGD        |
      | 5            | 01.2025  | 05.2034  | 06.2030     | 700   | ENDR         | Ja         | 1          | 0                  | UTVIDET_BARNETRYGD        |