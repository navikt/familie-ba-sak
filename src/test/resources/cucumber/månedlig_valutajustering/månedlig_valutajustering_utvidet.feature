# language: no
# encoding: UTF-8

Egenskap: Månedlig valutajustering med utvidet andel

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | Behandlingsresultat | Behandlingsårsak | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        | DELVIS_INNVILGET    | SØKNAD           | EØS                 | AVSLUTTET         |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 31.03.1989  |
      | 1            | 2       | BARN       | 11.05.2018  |

  Scenario: Skal differanseberegne utvidet andel selv om ordinær andel ikke utbetales
    Og dagens dato er 04.03.2026
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår             | Utdypende vilkår                            | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD     |                                             | 28.10.2025 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | UTVIDET_BARNETRYGD |                                             | 28.10.2025 |            | OPPFYLT  | Nei                  |                      |                  |
      | 1       | BOSATT_I_RIKET     | ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING | 28.10.2025 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | UNDER_18_ÅR        |                                             | 11.05.2018 | 10.05.2036 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP   |                                             | 11.05.2018 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET     | BARN_BOR_I_EØS                              | 28.10.2025 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER      | BARN_BOR_I_EØS_MED_SØKER                    | 28.10.2025 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD     |                                             | 28.10.2025 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.11.2025 |          | NORGE_ER_SEKUNDÆRLAND | 1            | I_ARBEID         | ARBEIDER                  | NL                    | NO                             | NL                  |

    Og med utenlandsk periodebeløp
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp   | Valuta kode | Intervall   | Utbetalingsland |
      | 2       | 11.2025   | 11.2025   | 1            | 888.49  | EUR         | KVARTALSVIS | NL              |
      | 2       | 12.2025   |           | 1            | 1821.05 | EUR         | KVARTALSVIS | NL              |

    Og med valutakurser
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs    | Vurderingsform |
      | 2       | 01.11.2025 | 30.11.2025 | 1            | 2025-10-31     | EUR         | 11.492  | AUTOMATISK     |
      | 2       | 01.12.2025 | 31.12.2025 | 1            | 2025-11-28     | EUR         | 11.7645 | AUTOMATISK     |
      | 2       | 01.01.2026 | 31.01.2026 | 1            | 2025-12-31     | EUR         | 11.843  | AUTOMATISK     |
      | 2       | 01.02.2026 |            | 1            | 2026-01-30     | EUR         | 11.3885 | AUTOMATISK     |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 1            | 01.11.2025 | 31.01.2026 | ALLEREDE_UTBETALT | 0       | 15.06.2018       |                             |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.11.2025 | 30.11.2025 | 1081  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 1            | 01.12.2025 | 31.01.2026 | 0     | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 1            | 01.02.2026 | 30.04.2036 | 0     | UTVIDET_BARNETRYGD | 100     | 2572 |
      | 2       | 1            | 01.11.2025 | 30.11.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 1            | 01.12.2025 | 31.12.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 1            | 01.01.2026 | 31.01.2026 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 1            | 01.02.2026 | 30.04.2036 | 0     | ORDINÆR_BARNETRYGD | 100     | 2012 |

    Når vi lager automatisk behandling med id 2 på fagsak 1 på grunn av automatisk valutajustering og har følgende valutakurser
      | Valuta kode | Valutakursdato | Kurs    |
      | EUR         | 2025-10-31     | 11.492  |
      | EUR         | 2025-11-28     | 11.7645 |
      | EUR         | 2025-12-31     | 11.843  |
      | EUR         | 2025-12-31     | 11.843  |
      | EUR         | 2026-01-30     | 11.3885 |
      | EUR         | 2026-02-27     | 11.2085 |

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats | Differanseberegnet beløp |
      | 1       | 2            | 01.11.2025 | 30.11.2025 | 1081  | UTVIDET_BARNETRYGD | 100     | 2516 | 1081                     |
      | 1       | 2            | 01.12.2025 | 31.01.2026 | 0     | UTVIDET_BARNETRYGD | 100     | 2516 | 0                        |
      | 1       | 2            | 01.02.2026 | 30.04.2036 | 0     | UTVIDET_BARNETRYGD | 100     | 2572 | 0                        |
      | 2       | 2            | 01.11.2025 | 30.11.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 | -1435                    |
      | 2       | 2            | 01.12.2025 | 31.12.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 | -5173                    |
      | 2       | 2            | 01.01.2026 | 31.01.2026 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 | -5220                    |
      | 2       | 2            | 01.02.2026 | 28.02.2026 | 0     | ORDINÆR_BARNETRYGD | 100     | 2012 | -4901                    |
      | 2       | 2            | 01.03.2026 | 30.04.2036 | 0     | ORDINÆR_BARNETRYGD | 100     | 2012 | -4791                    |

    Så forvent følgende valutakurser for behandling 2
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs    | Vurderingsform |
      | 2       | 01.11.2025 | 30.11.2025 | 2            | 2025-10-31     | EUR         | 11.492  | AUTOMATISK     |
      | 2       | 01.12.2025 | 31.12.2025 | 2            | 2025-11-28     | EUR         | 11.7645 | AUTOMATISK     |
      | 2       | 01.01.2026 | 31.01.2026 | 2            | 2025-12-31     | EUR         | 11.843  | AUTOMATISK     |
      | 2       | 01.02.2026 | 28.02.2026 | 2            | 2026-01-30     | EUR         | 11.3885 | AUTOMATISK     |
      | 2       | 01.03.2026 |            | 2            | 2026-02-27     | EUR         | 11.2085 | AUTOMATISK     |
