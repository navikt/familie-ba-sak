# language: no
# encoding: UTF-8

Egenskap: Plassholdertekst for egenskap - YgRlmz4HFf

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId  | Fagsaktype  |
      | 200059706 | INSTITUSJON |

    Gitt følgende behandling
      | BehandlingId | FagsakId  | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 100178154    | 200059706 |                     | IKKE_VURDERT        | SØKNAD           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId       | Persontype | Fødselsdato |
      | 100178154    | 2858550221151 | BARN       | 23.09.2004  |

  Scenario: Plassholdertekst for scenario - PgSlUF2LPI
    Og følgende dagens dato 24.09.2023
    Og lag personresultater for begrunnelse for behandling 100178154

    Og legg til nye vilkårresultater for begrunnelse for behandling 100178154
      | AktørId       | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2858550221151 | UNDER_18_ÅR                                    |                  | 23.09.2004 | 22.09.2022 | OPPFYLT  | Nei                  |
      | 2858550221151 | BOSATT_I_RIKET,GIFT_PARTNERSKAP,LOVLIG_OPPHOLD |                  | 23.09.2004 |            | OPPFYLT  | Nei                  |
      | 2858550221151 | BOR_MED_SØKER                                  |                  | 28.11.2019 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2858550221151 | 100178154    | 01.12.2019 | 31.05.2020 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 2858550221151 | 100178154    | 01.06.2020 | 31.08.2022 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |

    Og med endrede utbetalinger for begrunnelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent |
      | 2858550221151 | 100178154    | 01.12.2019 | 01.05.2020 | ETTERBETALING_3ÅR | 0       |

    Når begrunnelsetekster genereres for behandling 100178154

    Så forvent følgende standardBegrunnelser
      | Fra dato | Til dato | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser | Ekskluderte Begrunnelser |