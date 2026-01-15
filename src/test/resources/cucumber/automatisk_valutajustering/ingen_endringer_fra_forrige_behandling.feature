# language: no
# encoding: UTF-8

Egenskap: Automatisk valutajustering

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak         | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus | Behandlingssteg      | Behandlingstype |
      | 1            | 1        |                     | ENDRET_UTBETALING   | MÅNEDLIG_VALUTAJUSTERING | Ja                        | EØS                 | AVSLUTTET         | BEHANDLING_AVSLUTTET | REVURDERING     |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | ÅRLIG_KONTROLL           | Nei                       | EØS                 | UTREDES           | VILKÅRSVURDERING     | REVURDERING     |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 08.06.1984  |
      | 1            | 2       | BARN       | 18.03.2011  |
      | 2            | 1       | SØKER      | 08.06.1984  |
      | 2            | 2       | BARN       | 18.03.2011  |

  Scenario: Skal oppdatere andel tilkjent ytelse når vilkårsvurderingsteget utføres, selv om det ikke er noen endring
    Og dagens dato er 24.08.2024
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og fyll ut vikårresultater for behandling 1 fra dato 01.12.2022
    Og kopier vilkårresultater fra behandling 1 til behandling 2

    Og med kompetanser
      | AktørId | Fra dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.01.2023 | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | LV                             | LV                  |
      | 2       | 01.01.2023 | NORGE_ER_SEKUNDÆRLAND | 2            | ARBEIDER         | I_ARBEID                  | NO                    | LV                             | LV                  |

    Og med utenlandsk periodebeløp
      | AktørId | Fra måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2       | 01.2023   | 1            | 25    | EUR         | MÅNEDLIG  | LV              |
      | 2       | 01.2023   | 2            | 25    | EUR         | MÅNEDLIG  | LV              |

    Og med valutakurser
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs    | Vurderingsform |
      | 2       | 01.01.2023 | 31.05.2024 | 1            | 2022-12-30     | EUR         | 10.5138 | MANUELL        |
      | 2       | 01.06.2024 | 30.06.2024 | 1            | 2024-05-31     | EUR         | 11.383  | AUTOMATISK     |
      | 2       | 01.07.2024 | 31.07.2024 | 1            | 2024-06-28     | EUR         | 11.3965 | AUTOMATISK     |
      | 2       | 01.08.2024 |            | 1            | 2024-07-31     | EUR         | 11.8175 | AUTOMATISK     |
      | 2       | 01.01.2023 |            | 2            | 2022-12-30     | EUR         | 10.5138 | MANUELL        |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.01.2023 | 28.02.2023 | 792   | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 821   | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 31.12.2023 | 1048  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 1            | 01.01.2024 | 31.05.2024 | 1248  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 1            | 01.06.2024 | 31.07.2024 | 1226  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 1            | 01.08.2024 | 28.02.2029 | 1215  | ORDINÆR_BARNETRYGD | 100     | 1510 |

    Når vi utfører vilkårsvurderingssteget for behandling 2

    Så forvent følgende valutakurser for behandling 2
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs    | Vurderingsform |
      | 2       | 01.01.2023 | 31.05.2024 | 2            | 2022-12-30     | EUR         | 10.5138 | MANUELL        |
      | 2       | 01.06.2024 | 30.06.2024 | 2            | 2024-05-31     | EUR         | 11.383  | AUTOMATISK     |
      | 2       | 01.07.2024 | 31.07.2024 | 2            | 2024-06-28     | EUR         | 11.3965 | AUTOMATISK     |
      | 2       | 01.08.2024 |            | 2            | 2024-07-31     | EUR         | 11.8175 | AUTOMATISK     |

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats | Differanseberegnet beløp |
      | 2       | 2            | 01.01.2023 | 28.02.2023 | 792   | ORDINÆR_BARNETRYGD | 100     | 1054 | 792                      |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 821   | ORDINÆR_BARNETRYGD | 100     | 1083 | 821                      |
      | 2       | 2            | 01.07.2023 | 31.12.2023 | 1048  | ORDINÆR_BARNETRYGD | 100     | 1310 | 1048                     |
      | 2       | 2            | 01.01.2024 | 31.05.2024 | 1248  | ORDINÆR_BARNETRYGD | 100     | 1510 | 1248                     |
      | 2       | 2            | 01.06.2024 | 31.07.2024 | 1226  | ORDINÆR_BARNETRYGD | 100     | 1510 | 1226                     |
      | 2       | 2            | 01.08.2024 | 31.08.2024 | 1215  | ORDINÆR_BARNETRYGD | 100     | 1510 | 1215                     |
      | 2       | 2            | 01.09.2024 | 30.04.2025 | 1471  | ORDINÆR_BARNETRYGD | 100     | 1766 | 1471                     |
      | 2       | 2            | 01.05.2025 | 31.01.2026 | 1673  | ORDINÆR_BARNETRYGD | 100     | 1968 | 1673                     |
      | 2       | 2            | 01.02.2026 | 28.02.2029 | 1717  | ORDINÆR_BARNETRYGD | 100     | 2012 | 1717                     |