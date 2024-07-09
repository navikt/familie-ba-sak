# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for fortsatt innvilget

  Scenario: Skal gi begrunnelser for fortsatt innvilget nasjonal, som stemmer med vilkårsvurdering
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | ENDRET_UTBETALING   | NYE_OPPLYSNINGER |
      | 2            | 1        | 1                   | FORTSATT_INNVILGET  | NYE_OPPLYSNINGER |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 31.10.1987  |
      | 1            | 2       | BARN       | 19.02.2011  |
      | 2            | 1       | SØKER      | 31.10.1987  |
      | 2            | 2       | BARN       | 19.02.2011  |

    Og dagens dato er 20.09.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                        | Utdypende vilkår   | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD                                |                    | 31.10.1987 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                                |                    | 31.10.1987 | 14.06.2023 | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                                | VURDERT_MEDLEMSKAP | 15.06.2023 |            | OPPFYLT  | Nei                  |

      | 2       | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOR_MED_SØKER |                    | 19.02.2011 |            | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET                                |                    | 19.02.2011 | 14.06.2023 | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                   |                    | 19.02.2011 | 18.02.2029 | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET                                | VURDERT_MEDLEMSKAP | 15.06.2023 |            | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                        | Utdypende vilkår   | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD                                |                    | 31.10.1987 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                                |                    | 31.10.1987 | 14.06.2023 | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                                | VURDERT_MEDLEMSKAP | 15.06.2023 |            | OPPFYLT  | Nei                  |

      | 2       | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOR_MED_SØKER |                    | 19.02.2011 |            | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET                                |                    | 19.02.2011 | 14.06.2023 | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                   |                    | 19.02.2011 | 18.02.2029 | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET                                | VURDERT_MEDLEMSKAP | 15.06.2023 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.03.2011 | 28.02.2019 | 970   | ORDINÆR_BARNETRYGD | 100     | 970  |
      | 2       | 1            | 01.03.2019 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 31.01.2029 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 2            | 01.03.2011 | 28.02.2019 | 970   | ORDINÆR_BARNETRYGD | 100     | 970  |
      | 2       | 2            | 01.03.2019 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.07.2023 | 31.01.2029 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                     | Ugyldige begrunnelser                                                                |
      |          |          | FORTSATT_INNVILGET |                                | FORTSATT_INNVILGET_MEDLEM_I_FOLKETRYGDEN | FORTSATT_INNVILGET_FORVARING_GIFT, FORTSATT_INNVILGET_FORTSATT_AVTALE_OM_DELT_BOSTED |
      |          |          | FORTSATT_INNVILGET | EØS_FORORDNINGEN               |                                          | FORTSATT_INNVILGET_PRIMÆRLAND_STANDARD                                               |

  Scenario: Skal få tekst tilhørende EØS sekundærland ved fortsatt innvilget EØS sekundærland
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak     | Skal behandles automatisk |
      | 1            | 1        |                     | INNVILGET           | HELMANUELL_MIGRERING | Nei                       |
      | 2            | 1        | 1                   | FORTSATT_INNVILGET  | ÅRLIG_KONTROLL       | Nei                       |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 13.08.1976  |
      | 1            | 2       | BARN       | 03.04.2007  |
      | 2            | 1       | SØKER      | 13.08.1976  |
      | 2            | 2       | BARN       | 03.04.2007  |

    Og dagens dato er 17.10.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 31.12.2021 |            | OPPFYLT  | Nei                  |
      | 1       | LOVLIG_OPPHOLD   |                              | 31.12.2021 |            | OPPFYLT  | Nei                  |

      | 2       | UNDER_18_ÅR      |                              | 03.04.2007 | 02.04.2025 | OPPFYLT  | Nei                  |
      | 2       | GIFT_PARTNERSKAP |                              | 03.04.2007 |            | OPPFYLT  | Nei                  |
      | 2       | LOVLIG_OPPHOLD   |                              | 31.12.2021 |            | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 31.12.2021 |            | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 31.12.2021 |            | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD   |                              | 31.12.2021 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 31.12.2021 |            | OPPFYLT  | Nei                  |

      | 2       | UNDER_18_ÅR      |                              | 03.04.2007 | 02.04.2025 | OPPFYLT  | Nei                  |
      | 2       | GIFT_PARTNERSKAP |                              | 03.04.2007 |            | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 31.12.2021 |            | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 31.12.2021 |            | OPPFYLT  | Nei                  |
      | 2       | LOVLIG_OPPHOLD   |                              | 31.12.2021 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.01.2022 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 31.03.2025 | 187   | ORDINÆR_BARNETRYGD | 100     | 1310 |

      | 2       | 2            | 01.01.2022 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.07.2023 | 31.03.2025 | 187   | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.01.2022 |          | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |
      | 2       | 01.01.2022 |          | NORGE_ER_SEKUNDÆRLAND | 2            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                     | Ugyldige begrunnelser |
      |          |          | FORTSATT_INNVILGET | EØS_FORORDNINGEN               | FORTSETT_INNVILGET_SEKUNDÆRLAND_STANDARD |                       |

  Scenario: Skal få tekster tilhørende EØS primærland og sekundærland ved fortsatt innvilget EØS med ulik kompetanse for barna
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk |
      | 1            | 1        |                     | ENDRET_UTBETALING   | SATSENDRING      | Ja                        |
      | 2            | 1        | 1                   | FORTSATT_INNVILGET  | ÅRLIG_KONTROLL   | Nei                       |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 16.12.1978  |
      | 1            | 2       | BARN       | 05.09.2007  |
      | 1            | 3       | BARN       | 01.11.2012  |
      | 1            | 4       | BARN       | 09.05.2019  |
      | 2            | 1       | SØKER      | 16.12.1978  |
      | 2            | 2       | BARN       | 05.09.2007  |
      | 2            | 3       | BARN       | 01.11.2012  |
      | 2            | 4       | BARN       | 09.05.2019  |

    Og dagens dato er 18.10.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,UTVIDET_BARNETRYGD |                  | 01.10.2022 |            | OPPFYLT  | Nei                  |

      | 2       | GIFT_PARTNERSKAP                                 |                  | 05.09.2007 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                      |                  | 05.09.2007 | 04.09.2025 | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD      |                  | 01.10.2022 |            | OPPFYLT  | Nei                  |

      | 3       | GIFT_PARTNERSKAP                                 |                  | 01.11.2012 |            | OPPFYLT  | Nei                  |
      | 3       | UNDER_18_ÅR                                      |                  | 01.11.2012 | 31.10.2030 | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD      |                  | 01.10.2022 |            | OPPFYLT  | Nei                  |

      | 4       | UNDER_18_ÅR                                      |                  | 09.05.2019 | 08.05.2037 | OPPFYLT  | Nei                  |
      | 4       | GIFT_PARTNERSKAP                                 |                  | 09.05.2019 |            | OPPFYLT  | Nei                  |
      | 4       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD      |                  | 01.10.2022 |            | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                            | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET                    | OMFATTET_AV_NORSK_LOVGIVNING | 01.10.2022 |            | OPPFYLT  | Nei                  |
      | 1       | UTVIDET_BARNETRYGD,LOVLIG_OPPHOLD |                              | 01.10.2022 |            | OPPFYLT  | Nei                  |

      | 2       | UNDER_18_ÅR                       |                              | 05.09.2007 | 04.09.2025 | OPPFYLT  | Nei                  |
      | 2       | GIFT_PARTNERSKAP                  |                              | 05.09.2007 |            | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER                     | BARN_BOR_I_NORGE_MED_SØKER   | 01.10.2022 |            | OPPFYLT  | Nei                  |
      | 2       | LOVLIG_OPPHOLD                    |                              | 01.10.2022 |            | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET                    | BARN_BOR_I_NORGE             | 01.10.2022 |            | OPPFYLT  | Nei                  |

      | 3       | GIFT_PARTNERSKAP                  |                              | 01.11.2012 |            | OPPFYLT  | Nei                  |
      | 3       | UNDER_18_ÅR                       |                              | 01.11.2012 | 31.10.2030 | OPPFYLT  | Nei                  |
      | 3       | BOSATT_I_RIKET                    | BARN_BOR_I_NORGE             | 01.10.2022 |            | OPPFYLT  | Nei                  |
      | 3       | LOVLIG_OPPHOLD                    |                              | 01.10.2022 |            | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER                     | BARN_BOR_I_EØS_MED_SØKER     | 01.10.2022 |            | OPPFYLT  | Nei                  |

      | 4       | UNDER_18_ÅR                       |                              | 09.05.2019 | 08.05.2037 | OPPFYLT  | Nei                  |
      | 4       | GIFT_PARTNERSKAP                  |                              | 09.05.2019 |            | OPPFYLT  | Nei                  |
      | 4       | BOR_MED_SØKER                     | BARN_BOR_I_NORGE_MED_SØKER   | 01.10.2022 |            | OPPFYLT  | Nei                  |
      | 4       | BOSATT_I_RIKET                    | BARN_BOR_I_NORGE             | 01.10.2022 |            | OPPFYLT  | Nei                  |
      | 4       | LOVLIG_OPPHOLD                    |                              | 01.10.2022 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.11.2022 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 1            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 1       | 1            | 01.07.2023 | 30.04.2037 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 1            | 01.11.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 31.08.2025 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 1            | 01.11.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 1            | 01.07.2023 | 31.10.2030 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 4       | 1            | 01.11.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 4       | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 4       | 1            | 01.07.2023 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 1            | 01.05.2025 | 30.04.2037 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

      | 1       | 2            | 01.11.2022 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 2            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 1       | 2            | 01.07.2023 | 30.04.2037 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 2            | 01.11.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.07.2023 | 31.08.2025 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 2            | 01.11.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 2            | 01.07.2023 | 31.10.2030 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 4       | 2            | 01.11.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 4       | 2            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 4       | 2            | 01.07.2023 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 2            | 01.05.2025 | 30.04.2037 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet                     | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 3, 2    | 01.11.2022 |          | NORGE_ER_SEKUNDÆRLAND | 2            | MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN | I_ARBEID                  | NO                    | PL                             | NO                  |
      | 4       | 01.11.2022 |          | NORGE_ER_PRIMÆRLAND   | 2            | MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN | I_ARBEID                  | NO                    | NO                             | NO                  |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                                             | Ugyldige begrunnelser |
      |          |          | FORTSATT_INNVILGET |                                | FORTSATT_INNVILGET_BOR_ALENE_MED_BARN                                            |                       |
      |          |          | FORTSATT_INNVILGET | EØS_FORORDNINGEN               | FORTSATT_INNVILGET_PRIMÆRLAND_STANDARD, FORTSETT_INNVILGET_SEKUNDÆRLAND_STANDARD |                       |


  Scenario: Skal kunne velge endret utbetalingsbegrunnelser med type IKKE_RELEVANT selvom vedtaksperiodetype er fortsatt innvilget
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | ENDRET_UTBETALING   | ÅRLIG_KONTROLL   | Nei                       | EØS                 |
      | 2            | 1        | 1                   | FORTSATT_INNVILGET  | ÅRLIG_KONTROLL   | Nei                       | EØS                 |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 27.03.1981  |              |
      | 1            | 2       | BARN       | 04.11.2011  |              |
      | 1            | 3       | BARN       | 23.11.2017  |              |
      | 2            | 1       | SØKER      | 27.03.1981  |              |
      | 2            | 2       | BARN       | 04.11.2011  |              |
      | 2            | 3       | BARN       | 23.11.2017  |              |

    Og dagens dato er 28.02.2024
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD   |                              | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | GIFT_PARTNERSKAP |                              | 04.11.2011 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR      |                              | 04.11.2011 | 03.11.2029 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 3       | GIFT_PARTNERSKAP |                              | 23.11.2017 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR      |                              | 23.11.2017 | 22.11.2035 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | LOVLIG_OPPHOLD   |                              | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD   |                              | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | GIFT_PARTNERSKAP |                              | 04.11.2011 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR      |                              | 04.11.2011 | 03.11.2029 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 3       | UNDER_18_ÅR      |                              | 23.11.2017 | 22.11.2035 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP |                              | 23.11.2017 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | LOVLIG_OPPHOLD   |                              | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.01.2022 | 31.12.2022 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.01.2023 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 31.12.2023 | 15    | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 1            | 01.01.2024 | 31.10.2029 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 1            | 01.01.2022 | 31.12.2022 | 553   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 1            | 01.01.2023 | 28.02.2023 | 381   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 428   | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 3       | 1            | 01.07.2023 | 31.10.2023 | 471   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.11.2023 | 31.12.2023 | 15    | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 1            | 01.01.2024 | 31.10.2035 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 |

      | 2       | 2            | 01.01.2022 | 31.12.2022 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.01.2023 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.07.2023 | 31.12.2023 | 15    | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 2            | 01.01.2024 | 31.10.2029 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 2            | 01.01.2022 | 31.12.2022 | 553   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 2            | 01.01.2023 | 28.02.2023 | 381   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 428   | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 3       | 2            | 01.07.2023 | 31.10.2023 | 471   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 2            | 01.11.2023 | 31.12.2023 | 15    | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 2            | 01.01.2024 | 31.01.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 2            | 01.02.2024 | 31.10.2035 | 0     | ORDINÆR_BARNETRYGD | 0       | 1510 |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak          | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 3       | 2            | 01.02.2024 | 31.10.2035 | ENDRE_MOTTAKER | 0       | 23.09.2023       |                             |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                       | Ugyldige begrunnelser |
      |          |          | FORTSATT_INNVILGET |                                | ENDRET_UTBETALING_REDUKSJON_ENDRE_MOTTAKER |                       |