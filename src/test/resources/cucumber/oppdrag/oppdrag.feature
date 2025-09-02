# language: no
# encoding: UTF-8

Egenskap: Utbetalingsoppdrag: Vedtak for førstegangsbehandling


  Scenario: Vedtak med en periode

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 03.2021  | 700   |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    | 1               |

    Så forvent følgende oppdaterte andeler
      | BehandlingId | Id | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 0  | 0          |                    | 1               |

  Scenario: Revurdering uten endring av andeler

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 03.2021  | 700   |
      | 1            | 04.2021  | 04.2021  | 700   |
      | 2            | 03.2021  | 03.2021  | 700   |
      | 2            | 04.2021  | 04.2021  | 700   |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    | 1               |
      | 1            | 04.2021  | 04.2021  |             | 700   | NY           | Nei        | 1          | 0                  | 1               |

    Så forvent følgende oppdaterte andeler
      | BehandlingId | Id | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 0  | 0          |                    | 1               |
      | 1            | 1  | 1          | 0                  | 1               |


  Scenario: Vedtak med to perioder

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 03.2021  | 700   |
      | 1            | 04.2021  | 05.2021  | 800   |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    | 1               |
      | 1            | 04.2021  | 05.2021  |             | 800   | NY           | Nei        | 1          | 0                  | 1               |

    Så forvent følgende oppdaterte andeler
      | BehandlingId | Id | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 0  | 0          |                    | 1               |
      | 1            | 1  | 1          | 0                  | 1               |


  Scenario: Revurdering som legger til en periode, simulering skal opphøre fra start for å kunne vise all historikk

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 03.2021  | 700   |
      | 2            | 03.2021  | 03.2021  | 700   |
      | 2            | 04.2021  | 04.2021  | 800   |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    | 1               |
      | 2            | 04.2021  | 04.2021  |             | 800   | ENDR         | Nei        | 1          | 0                  | 2               |

    Så forvent følgende oppdaterte andeler
      | BehandlingId | Id | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 0  | 0          |                    | 1               |

      | 2            | 1  | 0          |                    | 1               |
      | 2            | 2  | 1          | 0                  | 2               |

    Så forvent følgende simulering
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    | 1               |
      | 2            | 03.2021  | 03.2021  | 03.2021     | 700   | ENDR         | Ja         | 0          |                    | 2               |
      | 2            | 03.2021  | 03.2021  |             | 700   | ENDR         | Nei        | 1          | 0                  | 2               |
      | 2            | 04.2021  | 04.2021  |             | 800   | ENDR         | Nei        | 2          | 1                  | 2               |

  Scenario: 2 revurderinger som legger til en periode

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 03.2021  | 700   |

      | 2            | 03.2021  | 03.2021  | 700   |
      | 2            | 04.2021  | 04.2021  | 800   |

      | 3            | 03.2021  | 03.2021  | 700   |
      | 3            | 04.2021  | 04.2021  | 800   |
      | 3            | 05.2021  | 05.2021  | 900   |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 03.2021  | 03.2021  |             | 700   | NY           | Nei        | 0          |                    | 1               |
      | 2            | 04.2021  | 04.2021  |             | 800   | ENDR         | Nei        | 1          | 0                  | 2               |
      | 3            | 05.2021  | 05.2021  |             | 900   | ENDR         | Nei        | 2          | 1                  | 3               |

    Så forvent følgende oppdaterte andeler
      | BehandlingId | Id | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 0  | 0          |                    | 1               |

      | 2            | 1  | 0          |                    | 1               |
      | 2            | 2  | 1          | 0                  | 2               |

      | 3            | 3  | 0          |                    | 1               |
      | 3            | 4  | 1          | 0                  | 2               |
      | 3            | 5  | 2          | 1                  | 3               |

  Scenario: Endrer beløp fra april

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 06.2021  | 700   |

      | 2            | 03.2021  | 03.2021  | 700   |
      | 2            | 04.2021  | 06.2021  | 800   |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 03.2021  | 06.2021  |             | 700   | NY           | Nei        | 0          |                    | 1               |
      | 2            | 04.2021  | 06.2021  |             | 800   | ENDR         | Nei        | 1          | 0                  | 2               |

    Så forvent følgende oppdaterte andeler
      | BehandlingId | Id | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 0  | 0          |                    | 1               |

      | 2            | 1  | 0          |                    | 1               |
      | 2            | 2  | 1          | 0                  | 2               |


  Scenario: Endrer beløp fra start

    Gitt følgende tilkjente ytelser
      | BehandlingId | Fra dato | Til dato | Beløp |
      | 1            | 03.2021  | 06.2021  | 700   |

      | 2            | 03.2021  | 03.2021  | 800   |
      | 2            | 04.2021  | 06.2021  | 700   |

    Når beregner utbetalingsoppdrag

    Så forvent følgende utbetalingsoppdrag
      | BehandlingId | Fra dato | Til dato | Opphørsdato | Beløp | Kode endring | Er endring | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 03.2021  | 06.2021  |             | 700   | NY           | Nei        | 0          |                    | 1               |
      | 2            | 03.2021  | 03.2021  |             | 800   | ENDR         | Nei        | 1          | 0                  | 2               |
      | 2            | 04.2021  | 06.2021  |             | 700   | ENDR         | Nei        | 2          | 1                  | 2               |

    Så forvent følgende oppdaterte andeler
      | BehandlingId | Id | Periode id | Forrige periode id | Kildebehandling |
      | 1            | 0  | 0          |                    | 1               |

      | 2            | 1  | 1          | 0                  | 2               |
      | 2            | 2  | 2          | 1                  | 2               |
