# language: no
# encoding: UTF-8

Egenskap: Vedtak for førstegangsbehandling


  Scenario: Vedtak med en periode

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Kildebehandling |
      | 1            | 03.2021  | 03.2021  | 700   | 1               |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 1          |                    |


  Scenario: Vedtak med to perioder

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Kildebehandling |
      | 1            | 03.2021  | 03.2021  | 700   | 1               |
      | 1            | 04.2021  | 05.2021  | 800   | 1               |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 1          |                    |
      | 1            | 04.2021  | 05.2021  |             | 800   | NY           | Nei        | 2          | 1                  |


  Scenario: Revurdering som legger til en periode

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Kildebehandling |
      | 1            | 03.2021  | 03.2021  | 700   | 1               |
      | 2            | 03.2021  | 03.2021  | 700   | 1               |
      | 2            | 04.2021  | 04.2021  | 800   | 2               |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 1          |                    |
      | 2            | 04.2021  | 04.2021  |             | 800   | ENDR         | Nei        | 2          | 1                  |
#
#    Og forvent følgende tilkjente ytelser for behandling 1 med startdato 03.2021
#      | Fra dato | Til dato | Beløp | Periode id | Forrige periode id | Kilde behandling id |
#      | 03.2021  | 03.2021  | 700   | 1          |                    | 1                   |
#
#    Og forvent følgende tilkjente ytelser for behandling 2 med startdato 02.2021
#      | Fra dato | Til dato | Beløp | Periode id | Forrige periode id | Kilde behandling id |
#      |          |          | 0     | 1          |                    | 2                   |
#
#    Og forvent følgende tilkjente ytelser for behandling 3 med startdato 01.2021
#      | Fra dato | Til dato | Beløp | Periode id | Forrige periode id | Kilde behandling id |
#      | 02.2021  | 02.2021  | 800   | 2          | 1                  | 3                   |