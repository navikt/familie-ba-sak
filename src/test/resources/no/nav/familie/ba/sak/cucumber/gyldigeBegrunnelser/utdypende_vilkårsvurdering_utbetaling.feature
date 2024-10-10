# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for utdypende vilkårsvurdering med utbetaling

  Bakgrunn:
    Gitt følgende behandlinger
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 13.04.2020  |

  Scenario: Skal gi riktige begrunnelser for delt bosted med og uten deling
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat | Utdypende vilkår            |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 11.01.1970 |            | Oppfylt  |                             |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |                             |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |                             |
      | 3456    | BOR_MED_SØKER                                    | 13.04.2020 | 10.03.2021 | Oppfylt  | DELT_BOSTED                 |
      | 3456    | BOR_MED_SØKER                                    | 11.03.2021 | 13.04.2022 | Oppfylt  | DELT_BOSTED_SKAL_IKKE_DELES |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2021 | 500   | 1            |
      | 3456    | 01.04.2021 | 30.04.2022 | 1354  | 1            |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Gyldige begrunnelser                                          | Ugyldige begrunnelser |
      | 01.05.2020 | 31.03.2021 | UTBETALING         | INNVILGET_AVTALE_DELT_BOSTED_FÅR_FRA_FLYTTETIDSPUNKT          |                       |
      | 01.04.2021 | 31.04.2022 | UTBETALING         | INNVILGET_ANNEN_FORELDER_IKKE_SØKT_DELT_BARNETRYGD_ALLE_BARNA |                       |
      | 01.05.2022 |            | OPPHØR             | OPPHØR_FAST_BOSTED_AVTALE                                     |                       |

  Scenario: Skal gi riktige begrunnelser for utdypende vilkår vurdering annet grunnlag
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                          | Fra dato   | Til dato   | Resultat | Utdypende vilkår         |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                  | 11.01.1970 |            | Oppfylt  |                          |
      | 3456    | UNDER_18_ÅR                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |                          |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, BOR_MED_SØKER | 13.04.2020 |            | Oppfylt  |                          |
      | 3456    | LOVLIG_OPPHOLD                                  | 13.04.2020 | 10.03.2021 | Oppfylt  | VURDERING_ANNET_GRUNNLAG |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2021 | 1354  | 1            |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Gyldige begrunnelser                              | Ugyldige begrunnelser |
      | 01.05.2020 | 31.03.2021 | UTBETALING         | INNVILGET_BOSATT_I_RIKTET                         |                       |
      | 01.04.2021 |            | OPPHØR             | OPPHØR_IKKE_OPPHOLDSTILLATELSE_MER_ENN_12_MÅNEDER |                       |

  Scenario: Skal detektere endring i utdypende vilkår
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat | Utdypende vilkår         |
      | 1234    | LOVLIG_OPPHOLD                                                  | 11.01.1970 |            | Oppfylt  |                          |
      | 1234    | BOSATT_I_RIKET                                                  | 11.01.1970 | 10.03.2022 | Oppfylt  | VURDERING_ANNET_GRUNNLAG |

      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |                          |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2020 |            | Oppfylt  |                          |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2022 | 1354  | 1            |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Gyldige begrunnelser                     | Ugyldige begrunnelser |
      | 01.05.2020 | 31.03.2022 | UTBETALING         |                                          |                       |
      | 01.04.2022 |            | OPPHØR             | OPPHØR_UTENLANDSOPPHOLD_OVER_TRE_MÅNEDER |                       |


  Scenario: Skal få begrunnelser for EØS-sak som har utdypende vilkårsvurdering på utgjørende vilkår, men ikke utdypende vilkårsvurdering på sanity
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | ENDRET_UTBETALING   | SATSENDRING      | Ja                        | NASJONAL            |
      | 2            | 1        | 1                   | INNVILGET_OG_ENDRET | SØKNAD           | Nei                       | EØS                 |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 21.08.1992  |
      | 1            | 2       | BARN       | 06.06.2015  |
      | 1            | 3       | BARN       | 06.06.2015  |
      | 2            | 1       | SØKER      | 21.08.1992  |
      | 2            | 2       | BARN       | 06.06.2015  |
      | 2            | 3       | BARN       | 06.06.2015  |

    Og dagens dato er 18.12.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 01.04.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | GIFT_PARTNERSKAP              |                  | 06.06.2015 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR                   |                  | 06.06.2015 | 05.06.2033 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 01.04.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                 | DELT_BOSTED      | 01.04.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | GIFT_PARTNERSKAP              |                  | 06.06.2015 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                   |                  | 06.06.2015 | 05.06.2033 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED      | 01.04.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.04.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                        | Utdypende vilkår                       | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                                        | 01.04.2022 | 13.02.2023 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET                | OMFATTET_AV_NORSK_LOVGIVNING           | 14.02.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD                |                                        | 14.02.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | UTVIDET_BARNETRYGD            |                                        | 14.02.2023 |            | OPPFYLT  | Nei                  |                      |                  |

      | 2       | GIFT_PARTNERSKAP              |                                        | 06.06.2015 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                   |                                        | 06.06.2015 | 05.06.2033 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                                        | 01.04.2022 | 13.02.2023 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED                            | 01.04.2022 | 13.02.2023 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET                | BARN_BOR_I_NORGE                       | 14.02.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER                 | BARN_BOR_I_NORGE_MED_SØKER,DELT_BOSTED | 14.02.2023 | 17.08.2023 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD                |                                        | 14.02.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER                 | BARN_BOR_I_NORGE_MED_SØKER             | 18.08.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 3       | GIFT_PARTNERSKAP              |                                        | 06.06.2015 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR                   |                                        | 06.06.2015 | 05.06.2033 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER                 | DELT_BOSTED                            | 01.04.2022 | 13.02.2023 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                                        | 01.04.2022 | 13.02.2023 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | LOVLIG_OPPHOLD                |                                        | 14.02.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | BOR_MED_SØKER                 | DELT_BOSTED,BARN_BOR_I_NORGE_MED_SØKER | 14.02.2023 | 17.08.2023 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | BOSATT_I_RIKET                | BARN_BOR_I_NORGE                       | 14.02.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | BOR_MED_SØKER                 | BARN_BOR_I_NORGE_MED_SØKER             | 18.08.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3       | 1            | 01.05.2022 | 28.02.2023 | 527   | ORDINÆR_BARNETRYGD | 50      | 1054 |
      | 2       | 1            | 01.05.2022 | 28.02.2023 | 527   | ORDINÆR_BARNETRYGD | 50      | 1054 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 542   | ORDINÆR_BARNETRYGD | 50      | 1083 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 542   | ORDINÆR_BARNETRYGD | 50      | 1083 |
      | 3       | 1            | 01.07.2023 | 31.05.2033 | 655   | ORDINÆR_BARNETRYGD | 50      | 1310 |
      | 2       | 1            | 01.07.2023 | 31.05.2033 | 655   | ORDINÆR_BARNETRYGD | 50      | 1310 |

      | 1       | 2            | 01.03.2023 | 30.06.2023 | 1245  | UTVIDET_BARNETRYGD | 50      | 2489 |
      | 1       | 2            | 01.07.2023 | 31.08.2023 | 1258  | UTVIDET_BARNETRYGD | 50      | 2516 |
      | 1       | 2            | 01.09.2023 | 31.05.2033 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 3       | 2            | 01.05.2022 | 28.02.2023 | 527   | ORDINÆR_BARNETRYGD | 50      | 1054 |
      | 2       | 2            | 01.05.2022 | 28.02.2023 | 527   | ORDINÆR_BARNETRYGD | 50      | 1054 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 542   | ORDINÆR_BARNETRYGD | 50      | 1083 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 542   | ORDINÆR_BARNETRYGD | 50      | 1083 |
      | 3       | 2            | 01.07.2023 | 31.08.2023 | 655   | ORDINÆR_BARNETRYGD | 50      | 1310 |
      | 2       | 2            | 01.07.2023 | 31.08.2023 | 655   | ORDINÆR_BARNETRYGD | 50      | 1310 |
      | 3       | 2            | 01.09.2023 | 31.05.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 2            | 01.09.2023 | 31.05.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato | Resultat            | BehandlingId | Søkers aktivitet                     | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2, 3    | 01.03.2023 |          | NORGE_ER_PRIMÆRLAND | 2            | MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN | I_ARBEID                  | NO                    | NO                             | NO                  |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                 | Ugyldige begrunnelser |
      | 01.03.2023 | 30.06.2023 | UTBETALING         |                                |                                                      |                       |
      | 01.07.2023 | 31.08.2023 | UTBETALING         |                                |                                                      |                       |
      | 01.09.2023 | 31.05.2033 | UTBETALING         |                                | INNVILGET_ENIGHET_OM_OPPHØR_AV_AVTALE_OM_DELT_BOSTED |                       |
      | 01.06.2033 |            | OPPHØR             |                                |                                                      |                       |

