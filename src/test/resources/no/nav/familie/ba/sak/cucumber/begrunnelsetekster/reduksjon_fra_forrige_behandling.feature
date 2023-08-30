# language: no
# encoding: UTF-8

Egenskap: Reduksjon fra forrige behandling

  Bakgrunn:
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

  Scenario: Skal gi reduksjon fra forrige behandling-begrunnelser knyttet til bor med søker når bor med søker er innvilget en måned senere i revurdering
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