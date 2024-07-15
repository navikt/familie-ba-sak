# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser når skalVisesSelvOmIkkeEndring trigger er true

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | Nei                       | NASJONAL            |
      | 2            | 1        | 1                   | ENDRET_OG_OPPHØRT   | NYE_OPPLYSNINGER | Nei                       | NASJONAL            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 16.07.1985  |
      | 1            | 2       | BARN       | 10.01.2023  |
      | 2            | 1       | SØKER      | 16.07.1985  |
      | 2            | 2       | BARN       | 10.01.2023  |

  Scenario: INNVILGET_BOR_ALENE_MED_BARN-begrunnelsen skal vises når man har utvidet barnetrygd selv om det ikke er noen endringer i utvidet vilkåret
    Og dagens dato er 20.10.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET                                               |                  | 16.07.1985 | 15.06.2023 | OPPFYLT  | Nei                  |
      | 1       | UTVIDET_BARNETRYGD,LOVLIG_OPPHOLD                            |                  | 16.07.1985 |            | OPPFYLT  | Nei                  |

      | 2       | LOVLIG_OPPHOLD,GIFT_PARTNERSKAP,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 10.01.2023 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                                  |                  | 10.01.2023 | 09.01.2041 | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                                       | Utdypende vilkår         | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET                                               |                          | 16.07.1985 | 14.03.2023 | OPPFYLT  | Nei                  |
      | 1       | UTVIDET_BARNETRYGD,LOVLIG_OPPHOLD                            |                          | 16.07.1985 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                                               | VURDERING_ANNET_GRUNNLAG | 15.03.2023 | 15.06.2023 | OPPFYLT  | Nei                  |

      | 2       | BOR_MED_SØKER,LOVLIG_OPPHOLD,BOSATT_I_RIKET,GIFT_PARTNERSKAP |                          | 10.01.2023 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                                  |                          | 10.01.2023 | 09.01.2041 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.02.2023 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 1            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 2       | 1            | 01.02.2023 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |

      | 1       | 2            | 01.02.2023 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 2            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 2       | 2            | 01.02.2023 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser         | Ugyldige begrunnelser |
      | 01.04.2023 | 30.06.2023 | UTBETALING         |                                | INNVILGET_BOR_ALENE_MED_BARN |                       |
      | 01.07.2023 |            | OPPHØR             |                                |                              |                       |

  Scenario: INNVILGET_BOR_ALENE_MED_BARN-begrunnelsen skal ikke vises når man ikke har utvidet barnetrygd
    Og dagens dato er 20.10.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET, LOVLIG_OPPHOLD                               |                  | 16.07.1985 | 15.06.2023 | OPPFYLT  | Nei                  |
      | 1       | UTVIDET_BARNETRYGD                                           |                  | 16.07.1985 |            | OPPFYLT  | Nei                  |

      | 2       | LOVLIG_OPPHOLD,GIFT_PARTNERSKAP,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 10.01.2023 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                                  |                  | 10.01.2023 | 09.01.2041 | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                                       | Utdypende vilkår         | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET, LOVLIG_OPPHOLD                               |                          | 16.07.1985 | 14.03.2023 | OPPFYLT  | Nei                  |
      | 1       | UTVIDET_BARNETRYGD                                           |                          | 16.07.1985 | 14.03.2023 | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET, LOVLIG_OPPHOLD                               | VURDERING_ANNET_GRUNNLAG | 15.03.2023 | 15.06.2023 | OPPFYLT  | Nei                  |

      | 2       | BOR_MED_SØKER,LOVLIG_OPPHOLD,BOSATT_I_RIKET,GIFT_PARTNERSKAP |                          | 10.01.2023 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                                  |                          | 10.01.2023 | 09.01.2041 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.02.2023 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 1            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 2       | 1            | 01.02.2023 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |

      | 1       | 2            | 01.02.2023 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 2            | 01.03.2023 | 31.03.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 2       | 2            | 01.02.2023 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser | Ugyldige begrunnelser        |
      | 01.04.2023 | 30.06.2023 | UTBETALING         |                                |                      | INNVILGET_BOR_ALENE_MED_BARN |
      | 01.07.2023 |            | OPPHØR             |                                |                      |                              |