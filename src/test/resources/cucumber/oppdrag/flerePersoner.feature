# language: no
# encoding: UTF-8

Egenskap: Utbetalingsoppdrag: Vedtak med flere identer


  Scenario: Vedtak med to perioder på ulike identer

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Ident |
      | 1            | 03.2021  | 03.2021  | 700   | 1     |
      | 1            | 03.2021  | 03.2021  | 700   | 2     |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    | 1               |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 1          |                    | 1               |

    Så forvent følgende oppdaterte andeler
      | BehandlingId | Id | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 0  | 0          |                    | 1               |
      | 1            | 1  | 1          |                    | 1               |

  Scenario: Revurderer og legger til en periode på en av personene

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Ident |
      | 1            | 03.2021  | 03.2021  | 700   | 1     |
      | 1            | 03.2021  | 03.2021  | 700   | 2     |

      | 2            | 03.2021  | 03.2021  | 700   | 1     |
      | 2            | 04.2021  | 04.2021  | 800   | 1     |
      | 2            | 03.2021  | 03.2021  | 700   | 2     |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    | 1               |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 1          |                    | 1               |
      | 2            | 04.2021  | 04.2021  |             | 800   | ENDR         | Nei        | 2          | 0                  | 2               |

    Så forvent følgende oppdaterte andeler
      | BehandlingId | Id | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 0  | 0          |                    | 1               |
      | 1            | 1  | 1          |                    | 1               |

      | 2            | 2  | 0          |                    | 1               |
      | 2            | 3  | 2          | 0                  | 2               |
      | 2            | 4  | 1          |                    | 1               |