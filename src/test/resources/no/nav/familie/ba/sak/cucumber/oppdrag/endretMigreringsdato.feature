# language: no
# encoding: UTF-8

Egenskap: Utbetalingsoppdrag: Endring av migreringsdato


  Scenario: Endrer migreringsdato på en behandling før første fom

    Gitt følgende behandlingsinformasjon
      | BehandlingId | Endret migreringsdato |
      | 1            | 02.2021               |
      | 2            | 12.2020               |

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Behandlingstype         |
      | 1            | 03.2021  | 03.2021  | 700   | MIGRERING_FRA_INFOTRYGD |
      | 2            | 03.2021  | 03.2021  | 700   | MIGRERING_FRA_INFOTRYGD |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |

      | 2            | 03.2021  | 03.2021  | 01.2021     | 700   | ENDR         | Ja         | 0          |                    |
      | 2            | 03.2021  | 03.2021  |             | 700   | ENDR         | Nei        | 1          | 0                  |

  Scenario: Endrer migreringsdato på en behandling etter første fom

    Gitt følgende behandlingsinformasjon
      | BehandlingId | Endret migreringsdato |
      | 1            | 02.2021               |
      | 2            | 03.2021               |

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Behandlingstype         |
      | 1            | 03.2021  | 03.2021  | 700   | MIGRERING_FRA_INFOTRYGD |
      | 2            | 03.2021  | 03.2021  | 700   | MIGRERING_FRA_INFOTRYGD |

    Når beregner utbetalingsoppdrag

    Så forvent at en exception kastes for behandling 2

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
