# language: no
# encoding: UTF-8

Egenskap: Opphør fra forrige behandling

  Bakgrunn:
    Gitt følgende behandling
      | BehandlingId | FagsakId  | ForrigeBehandlingId |
      | 1            | 1         |                     |
      | 2            | 1         | 1                   |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId       | Persontype | Fødselsdato |
      | 1            | 1234          | SØKER      | 16.09.1984  |
      | 1            | 3456          | BARN       | 07.09.2019  |
      | 2            | 1234          | SØKER      | 16.09.1984  |
      | 2            | 3456          | BARN       | 07.09.2019  |

  Scenario: Skal gi opphør fra forrige behandling-begrunnelser knyttet til bor med søker
    Og lag personresultater for begrunnelse for behandling 1
    Og lag personresultater for begrunnelse for behandling 2

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | LOVLIG_OPPHOLD,BOSATT_I_RIKET                                |                  | 16.09.1984 |            | OPPFYLT  | Nei                  |

      | 3456    | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER,GIFT_PARTNERSKAP |                  | 07.09.2019 |            | OPPFYLT  | Nei                  |
      | 3456    | UNDER_18_ÅR                                                  |                  | 07.09.2019 | 06.09.2037 | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for begrunnelse for behandling 2
      | AktørId | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET,LOVLIG_OPPHOLD                  |                  | 16.09.1984 |            | OPPFYLT  | Nei                  |

      | 3456    | BOSATT_I_RIKET,LOVLIG_OPPHOLD,GIFT_PARTNERSKAP |                  | 07.09.2019 |            | OPPFYLT  | Nei                  |
      | 3456    | UNDER_18_ÅR                                    |                  | 07.09.2019 | 06.09.2037 | OPPFYLT  | Nei                  |
      | 3456    | BOR_MED_SØKER                                  |                  | 07.09.2020 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 3456    | 1            | 01.10.2019 | 31.08.2020 | 1054  | ORDINÆR_BARNETRYGD | 100     |
      | 3456    | 1            | 01.09.2020 | 31.08.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     |
      | 3456    | 1            | 01.09.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     |
      | 3456    | 1            | 01.01.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     |
      | 3456    | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     |
      | 3456    | 1            | 01.07.2023 | 31.08.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     |
      | 3456    | 1            | 01.09.2025 | 31.08.2037 | 1310  | ORDINÆR_BARNETRYGD | 100     |
      | 3456    | 2            | 01.10.2020 | 31.08.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     |
      | 3456    | 2            | 01.09.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     |
      | 3456    | 2            | 01.01.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     |
      | 3456    | 2            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     |
      | 3456    | 2            | 01.07.2023 | 31.08.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     |
      | 3456    | 2            | 01.09.2025 | 31.08.2037 | 1310  | ORDINÆR_BARNETRYGD | 100     |

    Når begrunnelsetekster genereres for behandling 2

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser          | Ekskluderte Begrunnelser |
      | 01.10.2019 | 30.09.2020 | OPPHØR             |           | OPPHØR_BARN_BODDE_IKKE_MED_SØKER |                          |
      | 01.10.2020 | 31.08.2021 | UTBETALING         |           |                                  |                          |
      | 01.09.2021 | 31.12.2021 | UTBETALING         |           |                                  |                          |
      | 01.01.2022 | 28.02.2023 | UTBETALING         |           |                                  |                          |
      | 01.03.2023 | 30.06.2023 | UTBETALING         |           |                                  |                          |
      | 01.07.2023 | 31.08.2025 | UTBETALING         |           |                                  |                          |
      | 01.09.2025 | 31.08.2037 | UTBETALING         |           |                                  |                          |
      | 01.09.2037 |            | OPPHØR             |           |                                  |                          |
