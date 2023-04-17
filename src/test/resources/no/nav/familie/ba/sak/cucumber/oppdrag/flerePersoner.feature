# language: no
# encoding: UTF-8

Egenskap: Vedtak med flere identer


  Scenario: Vedtak med to perioder på ulike identer

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Kildebehandling | Ident |
      | 1            | 03.2021  | 03.2021  | 700   | 1               | 1     |
      | 1            | 03.2021  | 03.2021  | 700   | 1               | 2     |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 1          |                    |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 2          |                    |


  Scenario: Revurderer og legger til en periode på en av personene

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Kildebehandling | Ident |
      | 1            | 03.2021  | 03.2021  | 700   | 1               | 1     |
      | 1            | 03.2021  | 03.2021  | 700   | 1               | 2     |

      | 2            | 03.2021  | 03.2021  | 700   | 1               | 1     |
      | 2            | 04.2021  | 04.2021  | 800   | 2               | 1     |
      | 2            | 03.2021  | 03.2021  | 700   | 1               | 2     |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 1          |                    |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 2          |                    |
      | 2            | 04.2021  | 04.2021  |             | 800   | ENDR         | Nei        | 3          | 1                  |


  Scenario: Revurderer og avkorter stønadsperiode på en av personene

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Kildebehandling | Ident |
      | 1            | 03.2021  | 03.2021  | 700   | 1               | 1     |
      | 1            | 03.2021  | 04.2021  | 700   | 1               | 2     |

      | 2            | 03.2021  | 03.2021  | 700   | 1               | 1     |
      | 2            | 03.2021  | 03.2021  | 700   | 2               | 2     |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 1          |                    |
      | 1            | 03.2021  | 04.2021  |             | 700   | NY           | Nei        | 2          |                    |
      | 2            | 03.2021  | 04.2021  | 03.2021     | 700   | ENDR         | Ja         | 2          |                    |
      | 2            | 03.2021  | 03.2021  |             | 700   | ENDR         | Nei        | 3          | 2                  |


