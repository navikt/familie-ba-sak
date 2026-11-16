# language: no
# encoding: UTF-8

Egenskap: Avgrenset simulering av alle kjeder med minst seks måneders historikk

  Bakgrunn:
    Gitt følgende feature toggles
      | BehandlingId | FeatureToggleId                             | Er togglet på |
      | 1            | familie-ba-sak.simuler-kun-endrede-perioder | Ja            |
      | 2            | familie-ba-sak.simuler-kun-endrede-perioder | Ja            |

  Scenariomal: Endring <endringsmåned> på søker simulerer også uendret barn fra seks måneder før <inneværende måned>
    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato        | Til dato              | Beløp | Ident | Ytelse             |
      | 1            | 01.2024         | 12.2026               | 1000  | 1     | ORDINÆR_BARNETRYGD |
      | 1            | 01.2024         | 12.2026               | 2000  | 2     | UTVIDET_BARNETRYGD |
      | 2            | 01.2024         | 12.2026               | 1000  | 1     | ORDINÆR_BARNETRYGD |
      | 2            | 01.2024         | <måned før endringen> | 2000  | 2     | UTVIDET_BARNETRYGD |
      | 2            | <endringsmåned> | 12.2026               | 2500  | 2     | UTVIDET_BARNETRYGD |

    Og inneværende måned er <inneværende måned>

    Når beregner utbetalingsoppdrag

    Så forvent følgende simulering
      | BehandlingId | Fra dato           | Til dato              | Opphørsdato        | Beløp | Ytelse             | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 01.2024            | 12.2026               |                    | 1000  | ORDINÆR_BARNETRYGD | NY           | Nei        | 0          |                    | 1               |
      | 1            | 01.2024            | 12.2026               |                    | 2000  | UTVIDET_BARNETRYGD | NY           | Nei        | 1          |                    | 1               |
      | 2            | 01.2024            | 12.2026               | <simuleringsstart> | 1000  | ORDINÆR_BARNETRYGD | ENDR         | Ja         | 0          |                    | 2               |
      | 2            | 01.2024            | 12.2026               | <simuleringsstart> | 2000  | UTVIDET_BARNETRYGD | ENDR         | Ja         | 1          |                    | 2               |
      | 2            | <simuleringsstart> | 12.2026               |                    | 1000  | ORDINÆR_BARNETRYGD | ENDR         | Nei        | 2          | 0                  | 2               |
      | 2            | <simuleringsstart> | <måned før endringen> |                    | 2000  | UTVIDET_BARNETRYGD | ENDR         | Nei        | 3          | 1                  | 2               |
      | 2            | <endringsmåned>    | 12.2026               |                    | 2500  | UTVIDET_BARNETRYGD | ENDR         | Nei        | 4          | 3                  | 2               |

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato        | Til dato | Opphørsdato | Beløp | Ytelse             | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 01.2024         | 12.2026  |             | 1000  | ORDINÆR_BARNETRYGD | NY           | Nei        | 0          |                    | 1               |
      | 1            | 01.2024         | 12.2026  |             | 2000  | UTVIDET_BARNETRYGD | NY           | Nei        | 1          |                    | 1               |
      | 2            | <endringsmåned> | 12.2026  |             | 2500  | UTVIDET_BARNETRYGD | ENDR         | Nei        | 2          | 1                  | 2               |

    Så forvent følgende oppdaterte andeler
      | BehandlingId | Id | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 0  | 0          |                    | 1               |
      | 1            | 1  | 1          |                    | 1               |
      | 2            | 2  | 0          |                    | 1               |
      | 2            | 3  | 1          |                    | 1               |
      | 2            | 4  | 2          | 1                  | 2               |

    Eksempler:
      | inneværende måned | endringsmåned | måned før endringen | simuleringsstart |
      | 12.2025           | 07.2025       | 06.2025             | 06.2025          |
      | 12.2025           | 12.2025       | 11.2025             | 06.2025          |
      | 12.2025           | 01.2026       | 12.2025             | 06.2025          |
      | 02.2026           | 03.2026       | 02.2026             | 08.2025          |

  Scenariomal: Første endring <endringsmåned> på eller før seksmånedersgrensen styrer simuleringsstart
    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato        | Til dato              | Beløp | Ident | Ytelse             |
      | 1            | 01.2024         | 12.2026               | 1000  | 1     | ORDINÆR_BARNETRYGD |
      | 1            | 01.2024         | 12.2026               | 2000  | 2     | UTVIDET_BARNETRYGD |
      | 2            | 01.2024         | 12.2026               | 1000  | 1     | ORDINÆR_BARNETRYGD |
      | 2            | 01.2024         | <måned før endringen> | 2000  | 2     | UTVIDET_BARNETRYGD |
      | 2            | <endringsmåned> | 12.2026               | 2500  | 2     | UTVIDET_BARNETRYGD |

    Og inneværende måned er 12.2025

    Når beregner utbetalingsoppdrag

    Så forvent følgende simulering
      | BehandlingId | Fra dato        | Til dato | Opphørsdato     | Beløp | Ytelse             | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 01.2024         | 12.2026  |                 | 1000  | ORDINÆR_BARNETRYGD | NY           | Nei        | 0          |                    | 1               |
      | 1            | 01.2024         | 12.2026  |                 | 2000  | UTVIDET_BARNETRYGD | NY           | Nei        | 1          |                    | 1               |
      | 2            | 01.2024         | 12.2026  | <endringsmåned> | 1000  | ORDINÆR_BARNETRYGD | ENDR         | Ja         | 0          |                    | 2               |
      | 2            | 01.2024         | 12.2026  | <endringsmåned> | 2000  | UTVIDET_BARNETRYGD | ENDR         | Ja         | 1          |                    | 2               |
      | 2            | <endringsmåned> | 12.2026  |                 | 1000  | ORDINÆR_BARNETRYGD | ENDR         | Nei        | 2          | 0                  | 2               |
      | 2            | <endringsmåned> | 12.2026  |                 | 2500  | UTVIDET_BARNETRYGD | ENDR         | Nei        | 3          | 1                  | 2               |

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato        | Til dato | Opphørsdato | Beløp | Ytelse             | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 01.2024         | 12.2026  |             | 1000  | ORDINÆR_BARNETRYGD | NY           | Nei        | 0          |                    | 1               |
      | 1            | 01.2024         | 12.2026  |             | 2000  | UTVIDET_BARNETRYGD | NY           | Nei        | 1          |                    | 1               |
      | 2            | <endringsmåned> | 12.2026  |             | 2500  | UTVIDET_BARNETRYGD | ENDR         | Nei        | 2          | 1                  | 2               |

    Eksempler:
      | endringsmåned | måned før endringen |
      | 03.2025       | 02.2025             |
      | 06.2025       | 05.2025             |

  Scenario: Uendret barn blir simulert også når seksmånedersgrensen allerede er en periodegrense
    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Ident | Ytelse             |
      | 1            | 01.2024  | 05.2025  | 1000  | 1     | ORDINÆR_BARNETRYGD |
      | 1            | 06.2025  | 12.2026  | 1100  | 1     | ORDINÆR_BARNETRYGD |
      | 1            | 01.2024  | 12.2026  | 2000  | 2     | UTVIDET_BARNETRYGD |
      | 2            | 01.2024  | 05.2025  | 1000  | 1     | ORDINÆR_BARNETRYGD |
      | 2            | 06.2025  | 12.2026  | 1100  | 1     | ORDINÆR_BARNETRYGD |
      | 2            | 01.2024  | 10.2025  | 2000  | 2     | UTVIDET_BARNETRYGD |
      | 2            | 11.2025  | 12.2026  | 2500  | 2     | UTVIDET_BARNETRYGD |

    Og inneværende måned er 12.2025

    Når beregner utbetalingsoppdrag

    Så forvent følgende simulering
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Ytelse             | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 01.2024  | 05.2025  |             | 1000  | ORDINÆR_BARNETRYGD | NY           | Nei        | 0          |                    | 1               |
      | 1            | 06.2025  | 12.2026  |             | 1100  | ORDINÆR_BARNETRYGD | NY           | Nei        | 1          | 0                  | 1               |
      | 1            | 01.2024  | 12.2026  |             | 2000  | UTVIDET_BARNETRYGD | NY           | Nei        | 2          |                    | 1               |
      | 2            | 06.2025  | 12.2026  | 06.2025     | 1100  | ORDINÆR_BARNETRYGD | ENDR         | Ja         | 1          | 0                  | 2               |
      | 2            | 01.2024  | 12.2026  | 06.2025     | 2000  | UTVIDET_BARNETRYGD | ENDR         | Ja         | 2          |                    | 2               |
      | 2            | 06.2025  | 12.2026  |             | 1100  | ORDINÆR_BARNETRYGD | ENDR         | Nei        | 3          | 1                  | 2               |
      | 2            | 06.2025  | 10.2025  |             | 2000  | UTVIDET_BARNETRYGD | ENDR         | Nei        | 4          | 2                  | 2               |
      | 2            | 11.2025  | 12.2026  |             | 2500  | UTVIDET_BARNETRYGD | ENDR         | Nei        | 5          | 4                  | 2               |

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Ytelse             | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 01.2024  | 05.2025  |             | 1000  | ORDINÆR_BARNETRYGD | NY           | Nei        | 0          |                    | 1               |
      | 1            | 06.2025  | 12.2026  |             | 1100  | ORDINÆR_BARNETRYGD | NY           | Nei        | 1          | 0                  | 1               |
      | 1            | 01.2024  | 12.2026  |             | 2000  | UTVIDET_BARNETRYGD | NY           | Nei        | 2          |                    | 1               |
      | 2            | 11.2025  | 12.2026  |             | 2500  | UTVIDET_BARNETRYGD | ENDR         | Nei        | 3          | 2                  | 2               |

  Scenario: Opphør på søker beholder historikk for søker og uendret barn i simuleringsvinduet
    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Ident | Ytelse             |
      | 1            | 01.2024  | 12.2026  | 1000  | 1     | ORDINÆR_BARNETRYGD |
      | 1            | 01.2024  | 12.2026  | 2000  | 2     | UTVIDET_BARNETRYGD |
      | 2            | 01.2024  | 12.2026  | 1000  | 1     | ORDINÆR_BARNETRYGD |
      | 2            | 01.2024  | 11.2025  | 2000  | 2     | UTVIDET_BARNETRYGD |

    Og inneværende måned er 12.2025

    Når beregner utbetalingsoppdrag

    Så forvent følgende simulering
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Ytelse             | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 01.2024  | 12.2026  |             | 1000  | ORDINÆR_BARNETRYGD | NY           | Nei        | 0          |                    | 1               |
      | 1            | 01.2024  | 12.2026  |             | 2000  | UTVIDET_BARNETRYGD | NY           | Nei        | 1          |                    | 1               |
      | 2            | 01.2024  | 12.2026  | 06.2025     | 1000  | ORDINÆR_BARNETRYGD | ENDR         | Ja         | 0          |                    | 2               |
      | 2            | 01.2024  | 12.2026  | 06.2025     | 2000  | UTVIDET_BARNETRYGD | ENDR         | Ja         | 1          |                    | 2               |
      | 2            | 06.2025  | 12.2026  |             | 1000  | ORDINÆR_BARNETRYGD | ENDR         | Nei        | 2          | 0                  | 2               |
      | 2            | 06.2025  | 11.2025  |             | 2000  | UTVIDET_BARNETRYGD | ENDR         | Nei        | 3          | 1                  | 2               |

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Ytelse             | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 01.2024  | 12.2026  |             | 1000  | ORDINÆR_BARNETRYGD | NY           | Nei        | 0          |                    | 1               |
      | 1            | 01.2024  | 12.2026  |             | 2000  | UTVIDET_BARNETRYGD | NY           | Nei        | 1          |                    | 1               |
      | 2            | 01.2024  | 12.2026  | 12.2025     | 2000  | UTVIDET_BARNETRYGD | ENDR         | Ja         | 1          |                    | 2               |

  Scenario: Månedlige beløpsendringer før seksmånedersgrensen sendes ikke på nytt i simuleringen
    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Ident | Ytelse             |
      | 1            | 05.2025  | 05.2025  | 100   | 1     | ORDINÆR_BARNETRYGD |
      | 1            | 06.2025  | 06.2025  | 110   | 1     | ORDINÆR_BARNETRYGD |
      | 1            | 07.2025  | 07.2025  | 120   | 1     | ORDINÆR_BARNETRYGD |
      | 1            | 08.2025  | 08.2025  | 130   | 1     | ORDINÆR_BARNETRYGD |
      | 1            | 09.2025  | 09.2025  | 140   | 1     | ORDINÆR_BARNETRYGD |
      | 1            | 10.2025  | 10.2025  | 150   | 1     | ORDINÆR_BARNETRYGD |
      | 1            | 11.2025  | 11.2025  | 160   | 1     | ORDINÆR_BARNETRYGD |
      | 1            | 12.2025  | 12.2025  | 170   | 1     | ORDINÆR_BARNETRYGD |
      | 1            | 05.2025  | 12.2025  | 1000  | 2     | UTVIDET_BARNETRYGD |
      | 2            | 05.2025  | 05.2025  | 100   | 1     | ORDINÆR_BARNETRYGD |
      | 2            | 06.2025  | 06.2025  | 110   | 1     | ORDINÆR_BARNETRYGD |
      | 2            | 07.2025  | 07.2025  | 120   | 1     | ORDINÆR_BARNETRYGD |
      | 2            | 08.2025  | 08.2025  | 130   | 1     | ORDINÆR_BARNETRYGD |
      | 2            | 09.2025  | 09.2025  | 140   | 1     | ORDINÆR_BARNETRYGD |
      | 2            | 10.2025  | 10.2025  | 150   | 1     | ORDINÆR_BARNETRYGD |
      | 2            | 11.2025  | 11.2025  | 160   | 1     | ORDINÆR_BARNETRYGD |
      | 2            | 12.2025  | 12.2025  | 180   | 1     | ORDINÆR_BARNETRYGD |
      | 2            | 05.2025  | 12.2025  | 1000  | 2     | UTVIDET_BARNETRYGD |

    Og inneværende måned er 12.2025

    Når beregner utbetalingsoppdrag

    Så forvent følgende simulering
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Ytelse             | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 05.2025  | 05.2025  |             | 100   | ORDINÆR_BARNETRYGD | NY           | Nei        | 0          |                    | 1               |
      | 1            | 06.2025  | 06.2025  |             | 110   | ORDINÆR_BARNETRYGD | NY           | Nei        | 1          | 0                  | 1               |
      | 1            | 07.2025  | 07.2025  |             | 120   | ORDINÆR_BARNETRYGD | NY           | Nei        | 2          | 1                  | 1               |
      | 1            | 08.2025  | 08.2025  |             | 130   | ORDINÆR_BARNETRYGD | NY           | Nei        | 3          | 2                  | 1               |
      | 1            | 09.2025  | 09.2025  |             | 140   | ORDINÆR_BARNETRYGD | NY           | Nei        | 4          | 3                  | 1               |
      | 1            | 10.2025  | 10.2025  |             | 150   | ORDINÆR_BARNETRYGD | NY           | Nei        | 5          | 4                  | 1               |
      | 1            | 11.2025  | 11.2025  |             | 160   | ORDINÆR_BARNETRYGD | NY           | Nei        | 6          | 5                  | 1               |
      | 1            | 12.2025  | 12.2025  |             | 170   | ORDINÆR_BARNETRYGD | NY           | Nei        | 7          | 6                  | 1               |
      | 1            | 05.2025  | 12.2025  |             | 1000  | UTVIDET_BARNETRYGD | NY           | Nei        | 8          |                    | 1               |
      | 2            | 12.2025  | 12.2025  | 06.2025     | 170   | ORDINÆR_BARNETRYGD | ENDR         | Ja         | 7          | 6                  | 2               |
      | 2            | 05.2025  | 12.2025  | 06.2025     | 1000  | UTVIDET_BARNETRYGD | ENDR         | Ja         | 8          |                    | 2               |
      | 2            | 06.2025  | 06.2025  |             | 110   | ORDINÆR_BARNETRYGD | ENDR         | Nei        | 9          | 7                  | 2               |
      | 2            | 07.2025  | 07.2025  |             | 120   | ORDINÆR_BARNETRYGD | ENDR         | Nei        | 10         | 9                  | 2               |
      | 2            | 08.2025  | 08.2025  |             | 130   | ORDINÆR_BARNETRYGD | ENDR         | Nei        | 11         | 10                 | 2               |
      | 2            | 09.2025  | 09.2025  |             | 140   | ORDINÆR_BARNETRYGD | ENDR         | Nei        | 12         | 11                 | 2               |
      | 2            | 10.2025  | 10.2025  |             | 150   | ORDINÆR_BARNETRYGD | ENDR         | Nei        | 13         | 12                 | 2               |
      | 2            | 11.2025  | 11.2025  |             | 160   | ORDINÆR_BARNETRYGD | ENDR         | Nei        | 14         | 13                 | 2               |
      | 2            | 12.2025  | 12.2025  |             | 180   | ORDINÆR_BARNETRYGD | ENDR         | Nei        | 15         | 14                 | 2               |
      | 2            | 06.2025  | 12.2025  |             | 1000  | UTVIDET_BARNETRYGD | ENDR         | Nei        | 16         | 8                  | 2               |

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Ytelse             | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 05.2025  | 05.2025  |             | 100   | ORDINÆR_BARNETRYGD | NY           | Nei        | 0          |                    | 1               |
      | 1            | 06.2025  | 06.2025  |             | 110   | ORDINÆR_BARNETRYGD | NY           | Nei        | 1          | 0                  | 1               |
      | 1            | 07.2025  | 07.2025  |             | 120   | ORDINÆR_BARNETRYGD | NY           | Nei        | 2          | 1                  | 1               |
      | 1            | 08.2025  | 08.2025  |             | 130   | ORDINÆR_BARNETRYGD | NY           | Nei        | 3          | 2                  | 1               |
      | 1            | 09.2025  | 09.2025  |             | 140   | ORDINÆR_BARNETRYGD | NY           | Nei        | 4          | 3                  | 1               |
      | 1            | 10.2025  | 10.2025  |             | 150   | ORDINÆR_BARNETRYGD | NY           | Nei        | 5          | 4                  | 1               |
      | 1            | 11.2025  | 11.2025  |             | 160   | ORDINÆR_BARNETRYGD | NY           | Nei        | 6          | 5                  | 1               |
      | 1            | 12.2025  | 12.2025  |             | 170   | ORDINÆR_BARNETRYGD | NY           | Nei        | 7          | 6                  | 1               |
      | 1            | 05.2025  | 12.2025  |             | 1000  | UTVIDET_BARNETRYGD | NY           | Nei        | 8          |                    | 1               |
      | 2            | 12.2025  | 12.2025  |             | 180   | ORDINÆR_BARNETRYGD | ENDR         | Nei        | 9          | 7                  | 2               |

  Scenario: Toggle av beholder ordinær simulering fra første utbetaling for alle kjeder
    Gitt følgende feature toggles
      | BehandlingId | FeatureToggleId                             | Er togglet på |
      | 1            | familie-ba-sak.simuler-kun-endrede-perioder | Nei           |
      | 2            | familie-ba-sak.simuler-kun-endrede-perioder | Nei           |

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Ident | Ytelse             |
      | 1            | 01.2024  | 12.2026  | 1000  | 1     | ORDINÆR_BARNETRYGD |
      | 1            | 01.2024  | 12.2026  | 2000  | 2     | UTVIDET_BARNETRYGD |
      | 2            | 01.2024  | 12.2026  | 1000  | 1     | ORDINÆR_BARNETRYGD |
      | 2            | 01.2024  | 10.2025  | 2000  | 2     | UTVIDET_BARNETRYGD |
      | 2            | 11.2025  | 12.2026  | 2500  | 2     | UTVIDET_BARNETRYGD |

    Og inneværende måned er 12.2025

    Når beregner utbetalingsoppdrag

    Så forvent følgende simulering
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Ytelse             | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 01.2024  | 12.2026  |             | 1000  | ORDINÆR_BARNETRYGD | NY           | Nei        | 0          |                    | 1               |
      | 1            | 01.2024  | 12.2026  |             | 2000  | UTVIDET_BARNETRYGD | NY           | Nei        | 1          |                    | 1               |
      | 2            | 01.2024  | 12.2026  | 01.2024     | 1000  | ORDINÆR_BARNETRYGD | ENDR         | Ja         | 0          |                    | 2               |
      | 2            | 01.2024  | 12.2026  | 01.2024     | 2000  | UTVIDET_BARNETRYGD | ENDR         | Ja         | 1          |                    | 2               |
      | 2            | 01.2024  | 12.2026  |             | 1000  | ORDINÆR_BARNETRYGD | ENDR         | Nei        | 2          | 0                  | 2               |
      | 2            | 01.2024  | 10.2025  |             | 2000  | UTVIDET_BARNETRYGD | ENDR         | Nei        | 3          | 1                  | 2               |
      | 2            | 11.2025  | 12.2026  |             | 2500  | UTVIDET_BARNETRYGD | ENDR         | Nei        | 4          | 3                  | 2               |

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Ytelse             | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 01.2024  | 12.2026  |             | 1000  | ORDINÆR_BARNETRYGD | NY           | Nei        | 0          |                    | 1               |
      | 1            | 01.2024  | 12.2026  |             | 2000  | UTVIDET_BARNETRYGD | NY           | Nei        | 1          |                    | 1               |
      | 2            | 11.2025  | 12.2026  |             | 2500  | UTVIDET_BARNETRYGD | ENDR         | Nei        | 2          | 1                  | 2               |
