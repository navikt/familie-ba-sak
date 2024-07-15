# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for utvidet barnetrygd

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 26.04.1985  |
      | 1            | 2       | BARN       | 12.01.2022  |

  Scenario: Skal gi innvilgelsesbegrunnelse INNVILGET_BOR_ALENE_MED_BARN for utvidet i første utbetalingsperiode etter at utvidet er oppfylt
    Og dagens dato er 28.09.2023
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 26.04.1985 |            | OPPFYLT  | Nei                  |
      | 1       | UTVIDET_BARNETRYGD            |                  | 13.02.2023 |            | OPPFYLT  | Nei                  |

      | 2       | GIFT_PARTNERSKAP              |                  | 12.01.2022 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                   |                  | 12.01.2022 | 11.01.2040 | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER  |                  | 13.02.2023 |            | OPPFYLT  | Nei                  |
      | 2       | LOVLIG_OPPHOLD                |                  | 23.04.2023 | 30.06.2023 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.05.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 2       | 1            | 01.05.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Gyldige begrunnelser         | Ugyldige begrunnelser |
      | 01.05.2023 | 30.06.2023 | UTBETALING         |           | INNVILGET_BOR_ALENE_MED_BARN | INNVILGET_SKILT       |
      | 01.07.2023 |            | OPPHØR             |           |                              |                       |

  Scenario: Skal gi riktig begrunnelser når det ikke er utvidet pga. barnet bor i eøs med annen forelder
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak     |
      | 1            | 1        |                     | INNVILGET_OG_ENDRET | ENDRE_MIGRERINGSDATO |
      | 2            | 1        | 1                   | IKKE_VURDERT        | NYE_OPPLYSNINGER     |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 30.12.1983  |
      | 1            | 2       | BARN       | 05.02.2012  |
      | 1            | 3       | BARN       | 29.08.2013  |

      | 2            | 1       | SØKER      | 30.12.1983  |
      | 2            | 2       | BARN       | 05.02.2012  |
      | 2            | 3       | BARN       | 29.08.2013  |

    Og dagens dato er 01.01.2030
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                            | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | UTVIDET_BARNETRYGD,LOVLIG_OPPHOLD |                              | 01.10.2020 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                    | OMFATTET_AV_NORSK_LOVGIVNING | 15.12.2029 |            | OPPFYLT  | Nei                  |

      | 2       | UNDER_18_ÅR                       |                              | 05.02.2012 | 04.02.2030 | OPPFYLT  | Nei                  |
      | 2       | GIFT_PARTNERSKAP                  |                              | 05.02.2012 |            | OPPFYLT  | Nei                  |
      | 2       | LOVLIG_OPPHOLD                    |                              | 01.10.2020 |            | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET                    | BARN_BOR_I_NORGE             | 01.10.2020 |            | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER                     | BARN_BOR_I_NORGE_MED_SØKER   | 01.10.2020 |            | OPPFYLT  | Nei                  |

      | 3       | GIFT_PARTNERSKAP                  |                              | 29.08.2013 |            | OPPFYLT  | Nei                  |
      | 3       | UNDER_18_ÅR                       |                              | 29.08.2013 | 28.08.2031 | OPPFYLT  | Nei                  |
      | 3       | BOSATT_I_RIKET                    | BARN_BOR_I_NORGE             | 01.10.2020 |            | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER                     | BARN_BOR_I_NORGE_MED_SØKER   | 01.10.2020 |            | OPPFYLT  | Nei                  |
      | 3       | LOVLIG_OPPHOLD                    |                              | 01.10.2020 |            | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                      | Utdypende vilkår                  | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD,UTVIDET_BARNETRYGD           |                                   | 01.10.2020 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                              | OMFATTET_AV_NORSK_LOVGIVNING      | 15.12.2029 |            | OPPFYLT  | Nei                  |

      | 2       | UNDER_18_ÅR                                 |                                   | 05.02.2012 | 04.02.2030 | OPPFYLT  | Nei                  |
      | 2       | GIFT_PARTNERSKAP                            |                                   | 05.02.2012 |            | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER,LOVLIG_OPPHOLD |                                   | 01.10.2020 |            | OPPFYLT  | Nei                  |

      | 3       | GIFT_PARTNERSKAP                            |                                   | 29.08.2013 |            | OPPFYLT  | Nei                  |
      | 3       | UNDER_18_ÅR                                 |                                   | 29.08.2013 | 28.08.2031 | OPPFYLT  | Nei                  |
      | 3       | LOVLIG_OPPHOLD                              |                                   | 01.10.2020 |            | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER                               | BARN_BOR_I_EØS_MED_ANNEN_FORELDER | 01.10.2020 |            | OPPFYLT  | Nei                  |
      | 3       | BOSATT_I_RIKET                              | BARN_BOR_I_EØS                    | 01.10.2020 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.01.2030 | 31.07.2031 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 1            | 01.01.2030 | 31.01.2030 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 1            | 01.01.2030 | 31.07.2031 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

      | 1       | 2            | 01.01.2030 | 31.01.2030 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 2            | 01.01.2030 | 31.01.2030 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 2            | 01.01.2030 | 31.07.2031 | 51    | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2, 3    | 01.12.2022 |          | NORGE_ER_PRIMÆRLAND   | 1            | ARBEIDER         | INAKTIV                   | NO                    | SE                             | NO                  |
      | 3       | 01.12.2021 |          | NORGE_ER_SEKUNDÆRLAND | 2            | ARBEIDER         | I_ARBEID                  | NO                    | SE                             | SE                  |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Gyldige begrunnelser  | Ugyldige begrunnelser |
      | 01.01.2030 | 31.01.2030 | Utbetaling         |           |                       |                       |
      | 01.02.2030 | 31.07.2031 | Utbetaling         |           | REDUKSJON_UNDER_18_ÅR |                       |
      | 01.08.2031 |            | Opphør             |           |                       |                       |