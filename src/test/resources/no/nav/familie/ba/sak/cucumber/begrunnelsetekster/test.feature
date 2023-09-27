# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for endring i vilkår

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat  | Behandlingsårsak |
      | 1            | 1        |                     | INNVILGET_OG_OPPHØRT | SØKNAD           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | BARN       | 07.03.2016  |
      | 1            | 2       | SØKER      | 14.02.1972  |

  Scenario: Skal vise begrunnelse når vi aktivt lager en splitt i vilkåret
    Og følgende dagens dato 27.09.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                       | Fra dato   | Til dato   | Resultat |
      | 1       | UNDER_18_ÅR                                                  | 07.03.2016 | 06.03.2034 | OPPFYLT  |
      | 1       | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER | 07.03.2016 |            | OPPFYLT  |

      | 2       | LOVLIG_OPPHOLD                                               | 14.02.1972 |            | OPPFYLT  |
      | 2       | BOSATT_I_RIKET                                               | 15.12.2022 | 15.02.2023 | OPPFYLT  |
      | 2       | UTVIDET_BARNETRYGD                                           | 14.02.1972 | 14.01.2023 | OPPFYLT  |
      | 2       | UTVIDET_BARNETRYGD                                           | 15.01.2023 |            | OPPFYLT  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.01.2023 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.01.2023 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser      | Ekskluderte Begrunnelser |
      | 01.01.2023 | 31.01.2023 | UTBETALING         |           |                              |                          |
      | 01.02.2023 | 28.02.2023 | UTBETALING         |           | INNVILGET_BOR_ALENE_MED_BARN |                          |
      | 01.03.2023 |            | OPPHØR             |           |                              |                          |