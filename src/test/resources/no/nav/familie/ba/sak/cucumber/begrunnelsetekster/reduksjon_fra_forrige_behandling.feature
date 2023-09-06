# language: no
# encoding: UTF-8

Egenskap: Reduksjon fra forrige behandling

  Scenario: Skal gi reduksjon fra forrige behandling-begrunnelser knyttet til bor med søker når bor med søker er innvilget en måned senere i revurdering
    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId |
      | 1            | 1        |                     |
      | 2            | 1        | 1                   |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 31.12.1993  |
      | 1            | 2       | BARN       | 15.03.2023  |
      | 1            | 3       | BARN       | 15.03.2023  |
      | 2            | 1       | SØKER      | 31.12.1993  |
      | 2            | 2       | BARN       | 15.03.2023  |
      | 2            | 3       | BARN       | 15.03.2023  |

    Og lag personresultater for begrunnelse for behandling 1
    Og lag personresultater for begrunnelse for behandling 2

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD                                |                  | 15.03.2023 |            | OPPFYLT  | Nei                  |

      | 2       | UNDER_18_ÅR                                                  |                  | 15.03.2023 | 14.03.2041 | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER,GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 15.03.2023 | 30.06.2023 | OPPFYLT  | Nei                  |

      | 3       | UNDER_18_ÅR                                                  |                  | 15.03.2023 | 14.03.2041 | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER,GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 15.03.2023 | 30.06.2023 | OPPFYLT  | Nei                  |


    Og legg til nye vilkårresultater for begrunnelse for behandling 2
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD                                |                  | 15.03.2023 |            | OPPFYLT  | Nei                  |

      | 2       | LOVLIG_OPPHOLD,GIFT_PARTNERSKAP,BOSATT_I_RIKET               |                  | 15.03.2023 | 30.06.2023 | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                                  |                  | 15.03.2023 | 14.03.2041 | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER                                                |                  | 16.04.2023 |            | OPPFYLT  | Nei                  |

      | 3       | UNDER_18_ÅR                                                  |                  | 15.03.2023 | 14.03.2041 | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER,GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 15.03.2023 | 30.06.2023 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 2       | 1            | 01.04.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     |
      | 3       | 1            | 01.04.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     |
      | 2       | 2            | 01.04.2023 | 30.04.2023 | 1722  | ORDINÆR_BARNETRYGD | 100     |
      | 2       | 2            | 01.05.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     |
      | 3       | 2            | 01.04.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     |

    Når begrunnelsetekster genereres for behandling 2

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType                                      | Regelverk | Inkluderte Begrunnelser           | Ekskluderte Begrunnelser          |
      | 01.04.2023 | 30.04.2023 | UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING |           | REDUKSJON_BARN_BOR_IKKE_MED_SØKER | REDUKSJON_IKKE_OPPHOLDSTILLATELSE |
      | 01.05.2023 | 30.06.2023 | UTBETALING                                              |           |                                   |                                   |
      | 01.07.2023 |            | OPPHØR                                                  |           |                                   |                                   |

  Scenario: Skal gi reduksjon fra forrige behandling-begrunnelser knyttet til utvidet når utvidet ikke lenger er oppfylt
    Gitt følgende behandling
      | BehandlingId | FagsakId  | ForrigeBehandlingId |
      | 100173805    | 200056155 |                     |
      | 100173853    | 200056155 | 100173805           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId       | Persontype | Fødselsdato |
      | 100173805    | 2050306728585 | BARN       | 22.08.2022  |
      | 100173805    | 2586014455032 | SØKER      | 07.05.1985  |
      | 100173853    | 2050306728585 | BARN       | 22.08.2022  |
      | 100173853    | 2586014455032 | SØKER      | 07.05.1985  |

    Og lag personresultater for begrunnelse for behandling 100173805
    Og lag personresultater for begrunnelse for behandling 100173853

    Og legg til nye vilkårresultater for begrunnelse for behandling 100173805
      | AktørId       | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2586014455032 | BOSATT_I_RIKET,LOVLIG_OPPHOLD                                |                  | 07.05.1985 |            | OPPFYLT  | Nei                  |
      | 2586014455032 | UTVIDET_BARNETRYGD                                           |                  | 19.01.2023 |            | OPPFYLT  | Nei                  |

      | 2050306728585 | BOSATT_I_RIKET,LOVLIG_OPPHOLD,GIFT_PARTNERSKAP,BOR_MED_SØKER |                  | 22.08.2022 |            | OPPFYLT  | Nei                  |
      | 2050306728585 | UNDER_18_ÅR                                                  |                  | 22.08.2022 | 21.08.2040 | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for begrunnelse for behandling 100173853
      | AktørId       | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 2586014455032 | BOSATT_I_RIKET,LOVLIG_OPPHOLD                                |                  | 07.05.1985 |            | OPPFYLT      | Nei                  |
      | 2586014455032 | UTVIDET_BARNETRYGD                                           |                  | 19.01.2023 |            | IKKE_OPPFYLT | Nei                  |

      | 2050306728585 | BOR_MED_SØKER,LOVLIG_OPPHOLD,BOSATT_I_RIKET,GIFT_PARTNERSKAP |                  | 22.08.2022 |            | OPPFYLT      | Nei                  |
      | 2050306728585 | UNDER_18_ÅR                                                  |                  | 22.08.2022 | 21.08.2040 | OPPFYLT      | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 2050306728585 | 100173805    | 01.09.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     |
      | 2050306728585 | 100173805    | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     |
      | 2050306728585 | 100173805    | 01.07.2023 | 31.07.2028 | 1766  | ORDINÆR_BARNETRYGD | 100     |
      | 2050306728585 | 100173805    | 01.08.2028 | 31.07.2040 | 1310  | ORDINÆR_BARNETRYGD | 100     |
      | 2586014455032 | 100173805    | 01.02.2023 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     |
      | 2586014455032 | 100173805    | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     |
      | 2586014455032 | 100173805    | 01.07.2023 | 31.07.2040 | 2516  | UTVIDET_BARNETRYGD | 100     |
      | 2050306728585 | 100173853    | 01.09.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     |
      | 2050306728585 | 100173853    | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     |
      | 2050306728585 | 100173853    | 01.07.2023 | 31.07.2028 | 1766  | ORDINÆR_BARNETRYGD | 100     |
      | 2050306728585 | 100173853    | 01.08.2028 | 31.07.2040 | 1310  | ORDINÆR_BARNETRYGD | 100     |

    Når begrunnelsetekster genereres for behandling 100173853

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType                                      | Regelverk | Inkluderte Begrunnelser | Ekskluderte Begrunnelser |
      | 01.02.2023 | 28.02.2023 | UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING |           | REDUKSJON_SØKER_ER_GIFT |                          |
      | 01.03.2023 | 30.06.2023 | UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING |           | REDUKSJON_SØKER_ER_GIFT |                          |
      | 01.07.2023 | 31.07.2028 | UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING |           | REDUKSJON_SØKER_ER_GIFT |                          |
      | 01.08.2028 | 31.07.2040 | UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING |           | REDUKSJON_SØKER_ER_GIFT |                          |
      | 01.08.2040 |            | OPPHØR                                                  |           |                         |                          |
