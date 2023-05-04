# language: no
# encoding: UTF-8

Egenskap: Ulike ytelsestyper på andelene


  Scenario: Søker med utvidet og småbarnstillegg

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Ident | Ytelse             |
      | 1            | 03.2021  | 03.2021  | 700   | 1     | UTVIDET_BARNETRYGD |
      | 1            | 03.2021  | 03.2021  | 800   | 1     | SMÅBARNSTILLEGG    |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Ytelse             | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | UTVIDET_BARNETRYGD | NY           | Nei        | 0          |                    |
      | 1            | 03.2021  | 03.2021  |             | 800   | SMÅBARNSTILLEGG    | NY           | Nei        | 1          |                    |

  Scenario: Revurdering endrer beløp på småbarnstillegg fra april

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Ident | Ytelse             |
      | 1            | 03.2021  | 05.2021  | 700   | 1     | UTVIDET_BARNETRYGD |
      | 1            | 03.2021  | 05.2021  | 800   | 1     | SMÅBARNSTILLEGG    |
      | 2            | 03.2021  | 05.2021  | 700   | 1     | UTVIDET_BARNETRYGD |
      | 2            | 03.2021  | 03.2021  | 800   | 1     | SMÅBARNSTILLEGG    |
      | 2            | 04.2021  | 05.2021  | 800   | 1     | SMÅBARNSTILLEGG    |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Ytelse             | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 05.2021  |             | 700   | UTVIDET_BARNETRYGD | NY           | Nei        | 0          |                    |
      | 1            | 03.2021  | 05.2021  |             | 800   | SMÅBARNSTILLEGG    | NY           | Nei        | 1          |                    |
      | 2            | 03.2021  | 05.2021  | 03.2021     | 800   | SMÅBARNSTILLEGG    | ENDR         | Ja         | 1          |                    |
      | 2            | 03.2021  | 03.2021  |             | 800   | SMÅBARNSTILLEGG    | ENDR         | Nei        | 2          | 1                  |
      | 2            | 04.2021  | 05.2021  |             | 800   | SMÅBARNSTILLEGG    | ENDR         | Nei        | 3          | 2                  |
