# language: no
# encoding: UTF-8

Egenskap: Vedtak for førstegangsbehandling


  Scenario: Opphør en periode

    Gitt følgende tilkjente ytelser
      | BehandlingId | Uten andeler | Fra dato | Til dato | Beløp | Kildebehandling |
      | 1            |              | 03.2021  | 03.2021  | 700   | 1               |
      | 2            | Ja           |          |          |       |                 |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 2            | 03.2021  | 03.2021  | 03.2021     | 700   | ENDR         | Ja         | 0          |                    |

  Scenario: Iverksetter på nytt etter opphør

    Gitt følgende tilkjente ytelser
      | BehandlingId | Uten andeler | Fra dato | Til dato | Beløp | Kildebehandling |
      | 1            |              | 03.2021  | 03.2021  | 700   | 1               |
      | 2            | Ja           |          |          |       |                 |
      | 3            |              | 03.2021  | 03.2021  | 700   | 3               |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 2            | 03.2021  | 03.2021  | 03.2021     | 700   | ENDR         | Ja         | 0          |                    |
# TODO denne burde være feil, den burde vel kanskje peke til 0?
#      | 3            | 03.2021  | 03.2021  |             | 700   | ENDR         | Nei        | 1          | 0                  |
      | 3            | 03.2021  | 03.2021  |             | 700   | ENDR         | Nei        | 1          |                    |

  Scenario: Opphør en av 2 perioder

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Kildebehandling |
      | 1            | 03.2021  | 03.2021  | 700   | 1               |
      | 1            | 04.2021  | 04.2021  | 800   | 1               |
      | 2            | 03.2021  | 03.2021  | 700   | 1               |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 1            | 04.2021  | 04.2021  |             | 800   | NY           | Nei        | 1          | 0                  |
      | 2            | 04.2021  | 04.2021  | 04.2021     | 800   | ENDR         | Ja         | 1          | 0                  |

  Scenario: Opphører en lang periode

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Kildebehandling |
      | 1            | 03.2021  | 06.2021  | 700   | 1               |
      | 2            | 03.2021  | 04.2021  | 700   | 2               |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 06.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 2            | 03.2021  | 06.2021  | 03.2021     | 700   | ENDR         | Ja         | 0          |                    |
      | 2            | 03.2021  | 04.2021  |             | 700   | ENDR         | Nei        | 1          | 0                  |


  Scenario: 2 opphør etter hverendre på olike perioder

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Kildebehandling |
      | 1            | 03.2021  | 03.2021  | 700   | 1               |
      | 1            | 04.2021  | 04.2021  | 700   | 1               |
      | 1            | 05.2021  | 05.2021  | 700   | 1               |
      | 2            | 03.2021  | 03.2021  | 700   | 1               |
      | 2            | 04.2021  | 04.2021  | 700   | 1               |
      | 3            | 03.2021  | 03.2021  | 700   | 1               |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 1            | 04.2021  | 04.2021  |             | 700   | NY           | Nei        | 1          | 0                  |
      | 1            | 05.2021  | 05.2021  |             | 700   | NY           | Nei        | 2          | 1                  |

      | 2            | 05.2021  | 05.2021  | 05.2021     | 700   | ENDR         | Ja         | 2          | 1                  |

# TODO denne er egentligen feil? Det burde være den kommenterte koden som er riktig. Den burde peke til sen siste i kjeden, men opphøre fom 04.2021
#      | 3            | 05.2021  | 05.2021  | 04.2021     | 700   | ENDR         | Ja         | 2          | 1                  |
      | 3            | 04.2021  | 04.2021  | 04.2021     | 700   | ENDR         | Ja         | 1          | 0                  |

