# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for endret utbetaling

  Bakgrunn:
    Gitt følgende behandling
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 13.04.2020  |

  Scenario: Begrunnelse endret utbetaling delt bosted
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2020 |            | Oppfylt  |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | Fra dato   | Til dato   | BehandlingId | Årsak       | Prosent |
      | 3456    | 01.05.2020 | 31.01.2021 | 1            | DELT_BOSTED | 0       |
      | 3456    | 01.02.2021 | 31.03.2038 | 1            | DELT_BOSTED | 100     |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId | Prosent |
      | 3456    | 01.05.2020 | 31.01.2021 | 0     | 1            | 0       |
      | 3456    | 01.02.2021 | 31.03.2038 | 1354  | 1            | 100     |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Gyldige begrunnelser                                                                                                                | Ugyldige begrunnelser                                             |
      | 01.05.2020 | 31.01.2021 | UTBETALING         | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_KUN_ETTERBETALT_UTVIDET_NY                                                                    | ENDRET_UTBETALING_SEKUNDÆR_DELT_BOSTED_FULL_UTBETALING_FØR_SØKNAD |
      | 01.02.2021 | 31.03.2038 | UTBETALING         | ENDRET_UTBETALING_SEKUNDÆR_DELT_BOSTED_FULL_UTBETALING_FØR_SØKNAD, ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_KUN_ETTERBETALT_UTVIDET_NY |                                                                   |
      | 01.04.2038 |            | OPPHØR             | OPPHØR_UNDER_18_ÅR                                                                                                                  |                                                                   |

  Scenario: Begrunnelse etter endret utbetaling ETTERBETALING_3ÅR
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2020 |            | Oppfylt  |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | Fra dato   | Til dato   | BehandlingId | Årsak             | Prosent |
      | 3456    | 01.05.2020 | 31.01.2021 | 1            | ETTERBETALING_3ÅR | 0       |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId | Prosent |
      | 3456    | 01.05.2020 | 31.01.2021 | 0     | 1            | 0       |
      | 3456    | 01.02.2021 | 31.03.2038 | 1000  | 1            | 100     |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Gyldige begrunnelser                          | Ugyldige begrunnelser |
      | 01.02.2021 | 31.03.2038 | UTBETALING         | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_AAR |                          |
      | 01.04.2038 |            | OPPHØR             | OPPHØR_UNDER_18_ÅR                            |                          |


  Scenario: Skal ikke krasje dersom siste periode er endret til null prosent

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId |
      | 1            | 1        |                     |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | BARN       | 03.08.2017  |
      | 1            | 4567    | SØKER      | 05.06.1988  |

    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 4567    | BOSATT_I_RIKET,LOVLIG_OPPHOLD                  |                  | 05.06.1988 |            | OPPFYLT  | Nei                  |

      | 1234    | GIFT_PARTNERSKAP,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 03.08.2017 |            | OPPFYLT  | Nei                  |
      | 1234    | UNDER_18_ÅR                                    |                  | 03.08.2017 | 02.08.2035 | OPPFYLT  | Nei                  |
      | 1234    | BOR_MED_SØKER                                  |                  | 19.07.2023 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 1234    | 1            | 01.08.2023 | 31.08.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     |
      | 1234    | 1            | 01.09.2023 | 31.07.2035 | 0     | ORDINÆR_BARNETRYGD | 0       |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak          | Prosent |
      | 1234    | 1            | 01.09.2023 | 01.07.2035 | ENDRE_MOTTAKER | 0       |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Gyldige begrunnelser | Ugyldige begrunnelser |
      | 01.08.2023 | 31.08.2023 | UTBETALING         |           |                         |                          |
      | 01.09.2023 | 31.07.2035 | OPPHØR             |           |                         |                          |
      | 01.08.2035 |            | OPPHØR             |           |                         |                          |


  Scenario: Skal ikke ta med endret utbetalingsperioder som har type reduksjon dersom det ikke har vært en reduksjon
    Og følgende dagens dato 2023-09-13

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 4567    | BARN       | 02.02.2015  |
      | 1            | 1234    | SØKER      | 06.06.1985  |

    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET,LOVLIG_OPPHOLD                                |                  | 06.06.1985 |            | OPPFYLT  | Nei                  |

      | 4567    | UNDER_18_ÅR                                                  |                  | 02.02.2015 | 01.02.2033 | OPPFYLT  | Nei                  |
      | 4567    | GIFT_PARTNERSKAP,BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 02.02.2015 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 4567    | 1            | 01.03.2015 | 31.08.2020 | 0     | ORDINÆR_BARNETRYGD | 0       |
      | 4567    | 1            | 01.09.2020 | 31.01.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.02.2021 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.07.2023 | 31.01.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent |
      | 4567    | 1            | 01.03.2015 | 01.08.2020 | ETTERBETALING_3ÅR | 0       |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Gyldige begrunnelser                                 | Ugyldige begrunnelser                                                           |
      | 01.03.2015 | 31.08.2020 | OPPHØR             |           | ENDRET_UTBETALING_ETTERBETALING_TRE_ÅR_TILBAKE_I_TID | ENDRET_UTBETALING_ETTERBETALING_TRE_ÅR_TILBAKE_I_TID_KUN_UTVIDET_DEL_UTBETALING |
      | 01.09.2020 | 31.01.2021 | UTBETALING         |           |                                                      |                                                                                 |
      | 01.02.2021 | 28.02.2023 | UTBETALING         |           |                                                      |                                                                                 |
      | 01.03.2023 | 30.06.2023 | UTBETALING         |           |                                                      |                                                                                 |
      | 01.07.2023 | 31.01.2033 | UTBETALING         |           |                                                      |                                                                                 |
      | 01.02.2033 |            | OPPHØR             |           |                                                      |                                                                                 |

  Scenario: Skal begrunne endret utbetaling med årsak allerede er utbetalt, når det er utbetaling i perioden men ikke på personen med endret utbetaling
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | EØS                 |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 22.03.1982  |
      | 1            | 2       | BARN       | 10.04.2018  |
    Og følgende dagens dato 18.10.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                          | Utdypende vilkår                            | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD                  |                                             | 22.03.1982 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                  | ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING | 01.05.2023 |            | OPPFYLT  | Nei                  |
      | 1       | UTVIDET_BARNETRYGD              |                                             | 01.05.2023 |            | OPPFYLT  | Nei                  |

      | 2       | LOVLIG_OPPHOLD,GIFT_PARTNERSKAP |                                             | 10.04.2018 |            | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER                   | BARN_BOR_I_EØS_MED_SØKER                    | 10.04.2018 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                     |                                             | 10.04.2018 | 09.04.2036 | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET                  | BARN_BOR_I_EØS                              | 10.04.2018 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.06.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 1       | 1            | 01.07.2023 | 31.03.2036 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 1            | 01.06.2023 | 31.08.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 2       | 1            | 01.09.2023 | 31.03.2024 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.04.2024 | 31.03.2036 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent | Søknadstidspunkt |
      | 2       | 1            | 01.06.2023 | 31.08.2023 | ALLEREDE_UTBETALT | 0       | 01.06.2023       |

    Og med kompetanser for begrunnelse
      | AktørId | Fra dato   | Til dato | Resultat            | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.09.2023 |          | NORGE_ER_PRIMÆRLAND | 1            | INAKTIV          | ARBEIDER                  | BG                    | NO                             | BG                  |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Gyldige begrunnelser                                         | Ugyldige begrunnelser |
      | 01.06.2023 | 30.06.2023 | UTBETALING         |           | ENDRET_UTBETALING_SELVSTENDIG_RETT_ETTERBETALING_UTVIDET_DEL |                       |
      | 01.07.2023 | 31.08.2023 | UTBETALING         |           | INNVILGET_SATSENDRING                                        |                       |
      | 01.09.2023 | 31.03.2024 | UTBETALING         |           |                                                              |                       |
      | 01.04.2024 | 31.03.2036 | UTBETALING         |           |                                                              |                       |
      | 01.04.2036 |            | OPPHØR             |           |                                                              |                       |
