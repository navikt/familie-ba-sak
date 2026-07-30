# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser etter endret utbetaling med årsak endre mottaker

  Scenario: Begrunnelse skal være tilgjengelig i utbetalingsperioden etter en avsluttet endret utbetaling med årsak endre mottaker
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status    |
      | 1        | NORMAL     | AVSLUTTET |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | ENDRET_OG_OPPHØRT   | NYE_OPPLYSNINGER | Nei                       | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | INNVILGET_OG_ENDRET | SØKNAD           | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 30.06.1994  |              |
      | 1            | 2       | BARN       | 26.07.2021  |              |
      | 1            | 3       | BARN       | 20.02.2023  |              |
      | 2            | 1       | SØKER      | 30.06.1994  |              |
      | 2            | 2       | BARN       | 26.07.2021  |              |
      | 2            | 3       | BARN       | 20.02.2023  |              |

    Og dagens dato er 28.06.2026
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 2            | 3       |
      | 2            | 2       |
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 30.06.1994 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD            |                  | 27.07.2025 | 16.03.2026 | OPPFYLT      | Nei                  |                      |                  |
      | 1       | UTVIDET_BARNETRYGD            |                  | 17.03.2026 |            | IKKE_OPPFYLT | Nei                  |                      |                  |

      | 2       | UNDER_18_ÅR                   |                  | 26.07.2021 | 25.07.2039 | OPPFYLT      | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 26.07.2021 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |
      | 2       | GIFT_PARTNERSKAP              |                  | 26.07.2021 |            | OPPFYLT      | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER                 |                  | 27.07.2025 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |

      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 20.02.2023 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |
      | 3       | UNDER_18_ÅR                   |                  | 20.02.2023 | 19.02.2041 | OPPFYLT      | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP              |                  | 20.02.2023 |            | OPPFYLT      | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER                 |                  | 27.07.2025 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 30.06.1994 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD            |                  | 27.07.2025 | 16.03.2026 | OPPFYLT  | Nei                  |                      |                  |

      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 26.07.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | GIFT_PARTNERSKAP              |                  | 26.07.2021 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                   |                  | 26.07.2021 | 25.07.2039 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER                 |                  | 27.07.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | GIFT_PARTNERSKAP              |                  | 20.02.2023 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 20.02.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | UNDER_18_ÅR                   |                  | 20.02.2023 | 19.02.2041 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER                 |                  | 27.07.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak          | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 3,2     | 1            | 01.04.2026 | 30.06.2039 | ENDRE_MOTTAKER | 0       | 10.03.2026       |                             |
      | 3       | 1            | 01.07.2039 | 31.01.2041 | ENDRE_MOTTAKER | 0       | 10.03.2026       |                             |
      | 3,2     | 2            | 01.04.2026 | 30.06.2026 | ENDRE_MOTTAKER | 0       | 10.03.2026       |                             |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.08.2025 | 31.01.2026 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 1            | 01.02.2026 | 31.03.2026 | 2572  | UTVIDET_BARNETRYGD | 100     | 2572 |
      | 2       | 1            | 01.08.2025 | 31.01.2026 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 2       | 1            | 01.02.2026 | 31.03.2026 | 2012  | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 2       | 1            | 01.04.2026 | 30.06.2039 | 0     | ORDINÆR_BARNETRYGD | 0       | 2012 |
      | 3       | 1            | 01.08.2025 | 31.01.2026 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 1            | 01.02.2026 | 31.03.2026 | 2012  | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 3       | 1            | 01.04.2026 | 31.01.2041 | 0     | ORDINÆR_BARNETRYGD | 0       | 2012 |

      | 1       | 2            | 01.08.2025 | 31.01.2026 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 2            | 01.02.2026 | 31.03.2026 | 2572  | UTVIDET_BARNETRYGD | 100     | 2572 |
      | 2       | 2            | 01.08.2025 | 31.01.2026 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 2       | 2            | 01.02.2026 | 31.03.2026 | 2012  | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 2       | 2            | 01.04.2026 | 30.06.2026 | 0     | ORDINÆR_BARNETRYGD | 0       | 2012 |
      | 2       | 2            | 01.07.2026 | 30.06.2039 | 2012  | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 3       | 2            | 01.08.2025 | 31.01.2026 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 2            | 01.02.2026 | 31.03.2026 | 2012  | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 3       | 2            | 01.04.2026 | 30.06.2026 | 0     | ORDINÆR_BARNETRYGD | 0       | 2012 |
      | 3       | 2            | 01.07.2026 | 31.01.2041 | 2012  | ORDINÆR_BARNETRYGD | 100     | 2012 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Gyldige begrunnelser                   | Ugyldige begrunnelser |
      | 01.04.2026 | 30.06.2026 | OPPHØR             |                                        |                       |
      | 01.07.2026 | 30.06.2039 | UTBETALING         | ETTER_ENDRET_UTBETALING_ENDRE_MOTTAKER |                       |
      | 01.07.2039 | 31.01.2041 | UTBETALING         |                                        |                       |
      | 01.02.2041 |            | OPPHØR             |                                        |                       |
