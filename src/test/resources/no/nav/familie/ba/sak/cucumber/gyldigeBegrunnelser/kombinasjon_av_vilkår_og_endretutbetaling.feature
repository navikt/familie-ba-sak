# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for kombinasjon av utgjørende vilkår og endret utbetaling årsaker

  Bakgrunn:
    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId |
      | 1            | 1        |                     |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 11.02.1985  |
      | 1            | 2       | BARN       | 02.02.2015  |

  Scenario: Begrunnelse skal ikke vises dersom bare utgjørende vilkår er oppfylt
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår            | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                             | 01.02.2022 |            | OPPFYLT  | Nei                  |
      | 1       | UTVIDET_BARNETRYGD            |                             | 09.05.2022 |            | OPPFYLT  | Nei                  |

      | 2       | GIFT_PARTNERSKAP              |                             | 02.02.2015 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                   |                             | 02.02.2015 | 01.02.2033 | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                             | 01.02.2022 |            | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER                 |                             | 01.02.2022 | 08.05.2022 | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED_SKAL_IKKE_DELES | 09.05.2022 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 1       | 1            | 01.06.2022 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     |
      | 1       | 1            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     |
      | 1       | 1            | 01.07.2023 | 31.01.2033 | 2516  | UTVIDET_BARNETRYGD | 100     |
      | 2       | 1            | 01.03.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     |
      | 2       | 1            | 01.07.2023 | 31.01.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Gyldige begrunnelser | Ugyldige begrunnelser                                                             |
      | 01.03.2022 | 31.05.2022 | UTBETALING         |           |                         |                                                                                   |
      | 01.06.2022 | 28.02.2023 | UTBETALING         |           |                         | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_MOTTATT_FULL_ORDINÆR_ETTERBETALT_UTVIDET_NY |
      | 01.03.2023 | 30.06.2023 | UTBETALING         |           |                         |                                                                                   |
      | 01.07.2023 | 31.01.2033 | UTBETALING         |           |                         |                                                                                   |
      | 01.02.2033 |            | OPPHØR             |           |                         |                                                                                   |

  Scenario: Begrunnelse skal vises dersom både utgjørende vilkår er oppfylt og endret utbetalingsårsak er oppfylt
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår            | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                             | 01.02.2022 |            | OPPFYLT  | Nei                  |
      | 1       | UTVIDET_BARNETRYGD            |                             | 09.05.2022 |            | OPPFYLT  | Nei                  |

      | 2       | GIFT_PARTNERSKAP              |                             | 02.02.2015 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                   |                             | 02.02.2015 | 01.02.2033 | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                             | 01.02.2022 |            | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER                 |                             | 01.02.2022 | 08.05.2022 | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED_SKAL_IKKE_DELES | 09.05.2022 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 1       | 1            | 01.06.2022 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     |
      | 1       | 1            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     |
      | 1       | 1            | 01.07.2023 | 31.01.2033 | 2516  | UTVIDET_BARNETRYGD | 100     |
      | 2       | 1            | 01.03.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     |
      | 2       | 1            | 01.07.2023 | 31.01.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     |

    Og med endrede utbetalinger
      | AktørId | Fra dato   | Til dato   | BehandlingId | Årsak       | Prosent | Avtaletidspunkt delt bosted |
      | 2       | 01.06.2022 | 28.02.2023 | 1            | DELT_BOSTED | 100     | 02.02.2020                  |

    Når vedtaksperiodene genereres for behandling 1


    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Gyldige begrunnelser                                                              | Ugyldige begrunnelser |
      | 01.03.2022 | 31.05.2022 | UTBETALING         |           |                                                                                   |                          |
      | 01.06.2022 | 28.02.2023 | UTBETALING         |           | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_MOTTATT_FULL_ORDINÆR_ETTERBETALT_UTVIDET_NY |                          |
      | 01.03.2023 | 30.06.2023 | UTBETALING         |           |                                                                                   |                          |
      | 01.07.2023 | 31.01.2033 | UTBETALING         |           |                                                                                   |                          |
      | 01.02.2033 |            | OPPHØR             |           |                                                                                   |                          |
