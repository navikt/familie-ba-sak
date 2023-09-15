# language: no
# encoding: UTF-8

Egenskap: Plassholdertekst for egenskap - X8kVYFtgAK

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId  | Fagsaktype |
      | 200057901 | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId  | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 100176101    | 200057901 |                     | INNVILGET           | SØKNAD           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId       | Persontype | Fødselsdato |
      | 100176101    | 2274854533840 | SØKER      | 06.11.1984  |
      | 100176101    | 2610773691000 | BARN       | 07.09.2019  |

  Scenario: Plassholdertekst for scenario - 4QYTIiBxMY
    Og følgende dagens dato 15.09.2023
    Og lag personresultater for begrunnelse for behandling 100176101

    Og legg til nye vilkårresultater for begrunnelse for behandling 100176101
      | AktørId       | Vilkår                                      | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2610773691000 | UNDER_18_ÅR                                 |                              | 07.09.2019 | 06.09.2037 | OPPFYLT  | Nei                  |
      | 2610773691000 | GIFT_PARTNERSKAP                            |                              | 07.09.2019 |            | OPPFYLT  | Nei                  |
      | 2610773691000 | LOVLIG_OPPHOLD                              |                              | 07.09.2019 | 14.07.2023 | OPPFYLT  | Nei                  |
      | 2610773691000 | BOSATT_I_RIKET                              | BARN_BOR_I_NORGE             | 07.06.2023 | 14.07.2023 | OPPFYLT  | Nei                  |
      | 2610773691000 | BOR_MED_SØKER                               | BARN_BOR_I_EØS_MED_SØKER     | 07.09.2019 | 14.07.2023 | OPPFYLT  | Nei                  |
      | 2610773691000 | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER |                              | 15.07.2023 | 15.08.2023 | OPPFYLT  | Nei                  |

      | 2274854533840 | LOVLIG_OPPHOLD                              |                              | 06.11.1984 |            | OPPFYLT  | Nei                  |
      | 2274854533840 | BOSATT_I_RIKET                              | OMFATTET_AV_NORSK_LOVGIVNING | 11.11.2021 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2610773691000 | 100176101    | 01.07.2023 | 31.08.2023 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Og med kompetanser for begrunnelse
      | AktørId       | Fra dato   | Til dato   | Resultat            | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2610773691000 | 01.07.2023 | 31.08.2023 | NORGE_ER_PRIMÆRLAND | 100176101    | ARBEIDER         | I_ARBEID                  | NO                    | BE                             | BE                  |

    Når begrunnelsetekster genereres for behandling 100176101

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser                                | Ekskluderte Begrunnelser |
      | 01.07.2023 | 31.07.2023 | UTBETALING         |           |                                                        |                          |
      | 01.08.2023 | 31.08.2023 | UTBETALING         |           | INNVILGET_OVERGANG_EØS_TIL_NASJONAL_SEPARASJONSAVTALEN |                          |
      | 01.09.2023 |            | OPPHØR             |           |                                                        |                          |