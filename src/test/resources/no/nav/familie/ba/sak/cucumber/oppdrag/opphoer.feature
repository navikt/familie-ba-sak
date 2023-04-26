# language: no
# encoding: UTF-8

Egenskap: Opphør


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

    Så forvent følgende utbetalingsoppdrag med ny utbetalingsgenerator
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
      | 3            | 03.2021  | 03.2021  |             | 700   | ENDR         | Nei        | 1          |                    |

    Så forvent følgende utbetalingsoppdrag med ny utbetalingsgenerator
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 2            | 03.2021  | 03.2021  | 03.2021     | 700   | ENDR         | Ja         | 0          |                    |
      | 3            | 03.2021  | 03.2021  |             | 700   | ENDR         | Nei        | 1          | 0                  |

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

    Så forvent følgende utbetalingsoppdrag med ny utbetalingsgenerator
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

    Så forvent følgende utbetalingsoppdrag med ny utbetalingsgenerator
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 06.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 2            | 03.2021  | 06.2021  | 05.2021     | 700   | ENDR         | Ja         | 0          |                    |

  Scenario: Opphør en tidligere periode då vi kun har med den andre av 2 perioder

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Kildebehandling |
      | 1            | 03.2021  | 03.2021  | 700   | 1               |
      | 1            | 04.2021  | 04.2021  | 700   | 1               |
      | 2            | 04.2021  | 04.2021  | 700   | 2               |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 1            | 04.2021  | 04.2021  |             | 700   | NY           | Nei        | 1          | 0                  |

      | 2            | 04.2021  | 04.2021  | 03.2021     | 700   | ENDR         | Ja         | 1          | 0                  |
      | 2            | 04.2021  | 04.2021  |             | 700   | ENDR         | Nei        | 2          | 1                  |

    Så forvent følgende utbetalingsoppdrag med ny utbetalingsgenerator
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 1            | 04.2021  | 04.2021  |             | 700   | NY           | Nei        | 1          | 0                  |

      | 2            | 04.2021  | 04.2021  | 03.2021     | 700   | ENDR         | Ja         | 1          | 0                  |
      | 2            | 04.2021  | 04.2021  |             | 700   | ENDR         | Nei        | 2          | 1                  |

  Scenario: Endrer en tidligere periode til 0-utbetaling

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Kildebehandling |
      | 1            | 03.2021  | 03.2021  | 700   | 1               |
      | 1            | 04.2021  | 04.2021  | 700   | 1               |
      | 2            | 03.2021  | 03.2021  | 0     | 2               |
      | 2            | 04.2021  | 04.2021  | 700   | 2               |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 1            | 04.2021  | 04.2021  |             | 700   | NY           | Nei        | 1          | 0                  |

      | 2            | 04.2021  | 04.2021  | 03.2021     | 700   | ENDR         | Ja         | 1          | 0                  |
      | 2            | 04.2021  | 04.2021  |             | 700   | ENDR         | Nei        | 2          | 1                  |

    Så forvent følgende utbetalingsoppdrag med ny utbetalingsgenerator
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 1            | 04.2021  | 04.2021  |             | 700   | NY           | Nei        | 1          | 0                  |

      | 2            | 04.2021  | 04.2021  | 03.2021     | 700   | ENDR         | Ja         | 1          | 0                  |
      | 2            | 04.2021  | 04.2021  |             | 700   | ENDR         | Nei        | 2          | 1                  |


  Scenario: 2 opphør etter hverandre på ulike perioder

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Kildebehandling |
      | 1            | 03.2021  | 03.2021  | 700   | 1               |
      | 1            | 04.2021  | 04.2021  | 800   | 1               |
      | 1            | 05.2021  | 05.2021  | 900   | 1               |
      | 2            | 03.2021  | 03.2021  | 700   | 1               |
      | 2            | 04.2021  | 04.2021  | 800   | 1               |
      | 3            | 03.2021  | 03.2021  | 700   | 1               |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 1            | 04.2021  | 04.2021  |             | 800   | NY           | Nei        | 1          | 0                  |
      | 1            | 05.2021  | 05.2021  |             | 900   | NY           | Nei        | 2          | 1                  |

      | 2            | 05.2021  | 05.2021  | 05.2021     | 900   | ENDR         | Ja         | 2          | 1                  |

      | 3            | 04.2021  | 04.2021  | 04.2021     | 800   | ENDR         | Ja         | 1          | 0                  |

    Så forvent følgende utbetalingsoppdrag med ny utbetalingsgenerator
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 1            | 04.2021  | 04.2021  |             | 800   | NY           | Nei        | 1          | 0                  |
      | 1            | 05.2021  | 05.2021  |             | 900   | NY           | Nei        | 2          | 1                  |

      | 2            | 05.2021  | 05.2021  | 05.2021     | 900   | ENDR         | Ja         | 2          | 1                  |

      | 3            | 05.2021  | 05.2021  | 04.2021     | 900   | ENDR         | Ja         | 2          | 1                  |


  Scenario: Opphør mellom 2 andeler

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp | Kildebehandling |
      | 1            | 03.2021  | 08.2021  | 700   | 1               |
      | 2            | 03.2021  | 04.2021  | 700   | 1               |
      | 2            | 07.2021  | 08.2021  | 700   | 1               |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 08.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 2            | 03.2021  | 08.2021  | 03.2021     | 700   | ENDR         | Ja         | 0          |                    |
      | 2            | 03.2021  | 04.2021  |             | 700   | ENDR         | Nei        | 1          | 0                  |
      | 2            | 07.2021  | 08.2021  |             | 700   | ENDR         | Nei        | 2          | 1                  |

    Så forvent følgende utbetalingsoppdrag med ny utbetalingsgenerator
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id |
      | 1            | 03.2021  | 08.2021  |             | 700   | NY           | Nei        | 0          |                    |
      | 2            | 03.2021  | 08.2021  | 05.2021     | 700   | ENDR         | Ja         | 0          |                    |
      | 2            | 07.2021  | 08.2021  |             | 700   | ENDR         | Nei        | 1          | 0                  |