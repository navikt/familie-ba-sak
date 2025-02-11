# language: no
# encoding: UTF-8

Egenskap: Utbetalingsoppdrag: Automatisk revurdering med årsak OPPDATER_UTVIDET_KLASSEKODE

  Scenario: Skal sende kjedelementer for utvidet andeler fra andel som overlapper med inneværende måned og fremover

    Gitt følgende feature toggles
      | BehandlingId | FeatureToggleId                                                | Er togglet på |
      | 1            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Nei           |
      | 2            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Ja            |

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Ytelse             | Behandlingsårsak            |
      | 1            | 08.2024  | 05.2025  | 700   | UTVIDET_BARNETRYGD | SØKNAD                      |
      | 2            | 08.2024  | 05.2025  | 700   | UTVIDET_BARNETRYGD | OPPDATER_UTVIDET_KLASSEKODE |

    Og inneværende måned er 11.2024

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Ytelse                    |
      | 1            | 08.2024  | 05.2025  |             | 700   | NY           | Nei        | 0          |                    | UTVIDET_BARNETRYGD_GAMMEL |
      | 2            | 08.2024  | 05.2025  |             | 700   | ENDR         | Nei        | 1          | 0                  | UTVIDET_BARNETRYGD        |

  Scenario: Skal sende kjedelementer for utvidet andel når inneværende måned treffer i en andel og andelen også slutter inneværende måned

    Gitt følgende feature toggles
      | BehandlingId | FeatureToggleId                                                | Er togglet på |
      | 1            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Nei           |
      | 2            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Ja            |

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Ytelse             | Behandlingsårsak            |
      | 1            | 08.2024  | 11.2024  | 700   | UTVIDET_BARNETRYGD | SØKNAD                      |
      | 2            | 08.2024  | 11.2024  | 700   | UTVIDET_BARNETRYGD | OPPDATER_UTVIDET_KLASSEKODE |

    Og inneværende måned er 11.2024

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Ytelse                    |
      | 1            | 08.2024  | 11.2024  |             | 700   | NY           | Nei        | 0          |                    | UTVIDET_BARNETRYGD_GAMMEL |
      | 2            | 08.2024  | 11.2024  |             | 700   | ENDR         | Nei        | 1          | 0                  | UTVIDET_BARNETRYGD        |

  Scenario: Skal sende kjedelementer for alle utvidet andeler fra inneværende måned og fremover i tid når inneværende måned treffer midt i en andel

    Gitt følgende feature toggles
      | BehandlingId | FeatureToggleId                                                | Er togglet på |
      | 1            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Nei           |
      | 2            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Ja            |

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Ytelse             | Behandlingsårsak            |
      | 1            | 08.2024  | 05.2025  | 700   | UTVIDET_BARNETRYGD | SØKNAD                      |
      | 1            | 06.2025  | 12.2025  | 850   | UTVIDET_BARNETRYGD | SØKNAD                      |
      | 2            | 08.2024  | 02.2025  | 700   | UTVIDET_BARNETRYGD | OPPDATER_UTVIDET_KLASSEKODE |
      | 2            | 03.2025  | 05.2025  | 700   | UTVIDET_BARNETRYGD | OPPDATER_UTVIDET_KLASSEKODE |
      | 2            | 06.2025  | 12.2025  | 850   | UTVIDET_BARNETRYGD | OPPDATER_UTVIDET_KLASSEKODE |

    Og inneværende måned er 11.2024

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Ytelse                    |
      | 1            | 08.2024  | 05.2025  |             | 700   | NY           | Nei        | 0          |                    | UTVIDET_BARNETRYGD_GAMMEL |
      | 1            | 06.2025  | 12.2025  |             | 850   | NY           | Nei        | 1          | 0                  | UTVIDET_BARNETRYGD_GAMMEL |
      | 2            | 08.2024  | 02.2025  |             | 700   | ENDR         | Nei        | 2          | 1                  | UTVIDET_BARNETRYGD        |
      | 2            | 03.2025  | 05.2025  |             | 700   | ENDR         | Nei        | 3          | 2                  | UTVIDET_BARNETRYGD        |
      | 2            | 06.2025  | 12.2025  |             | 850   | ENDR         | Nei        | 4          | 3                  | UTVIDET_BARNETRYGD        |


  Scenario: Skal sende kjedelementer for alle utvidet andeler fra inneværende måned og fremover i tid når inneværende måned treffer et hull med utvidet andeler etter

    Gitt følgende feature toggles
      | BehandlingId | FeatureToggleId                                                | Er togglet på |
      | 1            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Nei           |
      | 2            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Ja            |

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Ytelse             | Behandlingsårsak            |
      | 1            | 05.2024  | 10.2024  | 600   | UTVIDET_BARNETRYGD | SØKNAD                      |
      | 1            | 02.2025  | 05.2025  | 700   | UTVIDET_BARNETRYGD | SØKNAD                      |
      | 2            | 05.2024  | 10.2024  | 600   | UTVIDET_BARNETRYGD | OPPDATER_UTVIDET_KLASSEKODE |
      | 2            | 02.2025  | 05.2025  | 700   | UTVIDET_BARNETRYGD | OPPDATER_UTVIDET_KLASSEKODE |

    Og inneværende måned er 11.2024

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Ytelse                    |
      | 1            | 05.2024  | 10.2024  |             | 600   | NY           | Nei        | 0          |                    | UTVIDET_BARNETRYGD_GAMMEL |
      | 1            | 02.2025  | 05.2025  |             | 700   | NY           | Nei        | 1          | 0                  | UTVIDET_BARNETRYGD_GAMMEL |
      | 2            | 02.2025  | 05.2025  |             | 700   | ENDR         | Nei        | 2          | 1                  | UTVIDET_BARNETRYGD        |