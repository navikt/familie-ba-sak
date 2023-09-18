# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser ved ingen endring

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 06.11.1984  |
      | 1            | 2       | BARN       | 07.09.2019  |

  Scenario: Gi innvilget-begrunnelser når det ikke er endring i andelene
    Og følgende dagens dato 15.09.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2       | UNDER_18_ÅR                                 |                              | 07.09.2019 | 06.09.2037 | OPPFYLT  | Nei                  |
      | 2       | GIFT_PARTNERSKAP                            |                              | 07.09.2019 |            | OPPFYLT  | Nei                  |
      | 2       | LOVLIG_OPPHOLD                              |                              | 07.09.2019 | 14.07.2023 | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET                              | BARN_BOR_I_NORGE             | 07.09.2019 | 14.07.2023 | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER                               | BARN_BOR_I_EØS_MED_SØKER     | 07.06.2023 | 14.07.2023 | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER |                              | 15.07.2023 | 15.08.2023 | OPPFYLT  | Nei                  |

      | 1       | LOVLIG_OPPHOLD                              |                              | 06.11.1984 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                              | OMFATTET_AV_NORSK_LOVGIVNING | 11.11.2021 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.07.2023 | 31.08.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Og med kompetanser for begrunnelse
      | AktørId | Fra dato   | Til dato   | Resultat            | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.07.2023 | 31.07.2023 | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | BE                             | BE                  |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser                                | Ekskluderte Begrunnelser |
      | 01.07.2023 | 31.07.2023 | UTBETALING         |           |                                                        |                          |
      | 01.08.2023 | 31.08.2023 | UTBETALING         |           | INNVILGET_OVERGANG_EØS_TIL_NASJONAL_SEPARASJONSAVTALEN |                          |
      | 01.09.2023 |            | OPPHØR             |           |                                                        |                          |