# language: no
# encoding: UTF-8

Egenskap: Plassholdertekst for egenskap - FmBuhcn0JU

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende vedtak
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak     |
      | 1            | 1        |                     | INNVILGET_OG_ENDRET | ENDRE_MIGRERINGSDATO |
      | 2            | 1909615  | 1                   | IKKE_VURDERT        | NYE_OPPLYSNINGER     |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 4       | BARN       | 05.02.2012  |
      | 1            | 2       | BARN       | 20.12.2004  |
      | 1            | 1       | SØKER      | 30.12.1983  |
      | 1            | 3       | BARN       | 08.11.2007  |
      | 1            | 5       | BARN       | 29.08.2013  |
      | 2            | 4       | BARN       | 05.02.2012  |
      | 2            | 2       | BARN       | 20.12.2004  |
      | 2            | 3       | BARN       | 08.11.2007  |
      | 2            | 1       | SØKER      | 30.12.1983  |
      | 2            | 5       | BARN       | 29.08.2013  |

  Scenario: Plassholdertekst for scenario - GblFGsLAao
    Og følgende dagens dato 06.11.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                            | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | UTVIDET_BARNETRYGD,LOVLIG_OPPHOLD |                              | 01.10.2020 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                    | OMFATTET_AV_NORSK_LOVGIVNING | 01.10.2020 |            | OPPFYLT  | Nei                  |

      | 2       | GIFT_PARTNERSKAP                  |                              | 20.12.2004 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                       |                              | 20.12.2004 | 19.12.2022 | OPPFYLT  | Nei                  |
      | 2       | LOVLIG_OPPHOLD                    |                              | 01.10.2020 |            | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER                     | BARN_BOR_I_NORGE_MED_SØKER   | 01.10.2020 |            | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET                    | BARN_BOR_I_NORGE             | 01.10.2020 |            | OPPFYLT  | Nei                  |

      | 3       | UNDER_18_ÅR                       |                              | 08.11.2007 | 07.11.2025 | OPPFYLT  | Nei                  |
      | 3       | GIFT_PARTNERSKAP                  |                              | 08.11.2007 |            | OPPFYLT  | Nei                  |
      | 3       | BOSATT_I_RIKET                    | BARN_BOR_I_NORGE             | 01.10.2020 |            | OPPFYLT  | Nei                  |
      | 3       | LOVLIG_OPPHOLD                    |                              | 01.10.2020 |            | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER                     | BARN_BOR_I_NORGE_MED_SØKER   | 01.10.2020 |            | OPPFYLT  | Nei                  |

      | 4       | UNDER_18_ÅR                       |                              | 05.02.2012 | 04.02.2030 | OPPFYLT  | Nei                  |
      | 4       | GIFT_PARTNERSKAP                  |                              | 05.02.2012 |            | OPPFYLT  | Nei                  |
      | 4       | LOVLIG_OPPHOLD                    |                              | 01.10.2020 |            | OPPFYLT  | Nei                  |
      | 4       | BOSATT_I_RIKET                    | BARN_BOR_I_NORGE             | 01.10.2020 |            | OPPFYLT  | Nei                  |
      | 4       | BOR_MED_SØKER                     | BARN_BOR_I_NORGE_MED_SØKER   | 01.10.2020 |            | OPPFYLT  | Nei                  |

      | 5       | GIFT_PARTNERSKAP                  |                              | 29.08.2013 |            | OPPFYLT  | Nei                  |
      | 5       | UNDER_18_ÅR                       |                              | 29.08.2013 | 28.08.2031 | OPPFYLT  | Nei                  |
      | 5       | BOSATT_I_RIKET                    | BARN_BOR_I_NORGE             | 01.10.2020 |            | OPPFYLT  | Nei                  |
      | 5       | BOR_MED_SØKER                     | BARN_BOR_I_NORGE_MED_SØKER   | 01.10.2020 |            | OPPFYLT  | Nei                  |
      | 5       | LOVLIG_OPPHOLD                    |                              | 01.10.2020 |            | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                      | Utdypende vilkår                  | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD,UTVIDET_BARNETRYGD           |                                   | 01.10.2020 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                              | OMFATTET_AV_NORSK_LOVGIVNING      | 01.10.2020 |            | OPPFYLT  | Nei                  |

      | 2       | GIFT_PARTNERSKAP                            |                                   | 20.12.2004 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                 |                                   | 20.12.2004 | 19.12.2022 | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                                   | 01.10.2020 |            | OPPFYLT  | Nei                  |

      | 3       | UNDER_18_ÅR                                 |                                   | 08.11.2007 | 07.11.2025 | OPPFYLT  | Nei                  |
      | 3       | GIFT_PARTNERSKAP                            |                                   | 08.11.2007 |            | OPPFYLT  | Nei                  |
      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER |                                   | 01.10.2020 |            | OPPFYLT  | Nei                  |

      | 4       | UNDER_18_ÅR                                 |                                   | 05.02.2012 | 04.02.2030 | OPPFYLT  | Nei                  |
      | 4       | GIFT_PARTNERSKAP                            |                                   | 05.02.2012 |            | OPPFYLT  | Nei                  |
      | 4       | BOSATT_I_RIKET,BOR_MED_SØKER,LOVLIG_OPPHOLD |                                   | 01.10.2020 |            | OPPFYLT  | Nei                  |

      | 5       | GIFT_PARTNERSKAP                            |                                   | 29.08.2013 |            | OPPFYLT  | Nei                  |
      | 5       | UNDER_18_ÅR                                 |                                   | 29.08.2013 | 28.08.2031 | OPPFYLT  | Nei                  |
      | 5       | LOVLIG_OPPHOLD                              |                                   | 01.10.2020 |            | OPPFYLT  | Nei                  |
      | 5       | BOR_MED_SØKER                               | BARN_BOR_I_EØS_MED_ANNEN_FORELDER | 01.10.2020 |            | OPPFYLT  | Nei                  |
      | 5       | BOSATT_I_RIKET                              | BARN_BOR_I_EØS                    | 01.10.2020 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 4       | 1            | 01.11.2020 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 4       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 4       | 1            | 01.07.2023 | 31.01.2030 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 1            | 01.11.2020 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 1            | 01.07.2023 | 31.10.2025 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 1            | 01.11.2020 | 30.11.2022 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 1       | 1            | 01.11.2020 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 1            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 1       | 1            | 01.07.2023 | 31.07.2031 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 5       | 1            | 01.11.2020 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 5       | 1            | 01.07.2023 | 31.07.2031 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 4       | 2            | 01.11.2020 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 4       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 4       | 2            | 01.07.2023 | 31.01.2030 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 2            | 01.11.2020 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 2            | 01.07.2023 | 31.10.2025 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 2            | 01.11.2020 | 30.11.2022 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 1       | 2            | 01.11.2020 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 2            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 1       | 2            | 01.07.2023 | 31.01.2030 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 5       | 2            | 01.11.2020 | 31.03.2021 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5       | 2            | 01.04.2021 | 31.05.2021 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5       | 2            | 01.06.2021 | 31.07.2021 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5       | 2            | 01.08.2021 | 30.11.2021 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5       | 2            | 01.12.2021 | 31.12.2021 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5       | 2            | 01.01.2022 | 31.12.2022 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5       | 2            | 01.01.2023 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5       | 2            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 5       | 2            | 01.07.2023 | 31.07.2031 | 51    | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med kompetanser
      | AktørId    | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2, 4, 3, 5 | 01.11.2020 | 30.11.2022 | NORGE_ER_PRIMÆRLAND   | 1            | ARBEIDER         | INAKTIV                   | NO                    | SE                             | NO                  |
      | 4, 3, 5    | 01.12.2022 |            | NORGE_ER_PRIMÆRLAND   | 1            | ARBEIDER         | INAKTIV                   | NO                    | SE                             | NO                  |
      | 5          | 01.12.2021 |            | NORGE_ER_SEKUNDÆRLAND | 2            | ARBEIDER         | I_ARBEID                  | NO                    | SE                             | SE                  |
      | 5          | 01.06.2021 | 31.07.2021 | NORGE_ER_SEKUNDÆRLAND | 2            | ARBEIDER         | I_ARBEID                  | NO                    | SE                             | SE                  |
      | 5          | 01.04.2021 | 31.05.2021 | NORGE_ER_PRIMÆRLAND   | 2            | ARBEIDER         | INAKTIV                   | NO                    | SE                             | SE                  |
      | 5          | 01.11.2020 | 31.03.2021 | NORGE_ER_SEKUNDÆRLAND | 2            | ARBEIDER         | I_ARBEID                  | NO                    | SE                             | SE                  |
      | 5          | 01.08.2021 | 30.11.2021 | NORGE_ER_PRIMÆRLAND   | 2            | ARBEIDER         | INAKTIV                   | NO                    | SE                             | SE                  |

    Når vedtaksperioder med begrunnelser genereres for behandling 2

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato | Til dato | Vedtaksperiodetype | Kommentar |
