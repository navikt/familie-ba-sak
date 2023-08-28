# language: no
# encoding: UTF-8

Egenskap: List has more than one element

  Bakgrunn:
    Gitt følgende behandling
      | BehandlingId | ForrigeBehandlingId |
      | 1            |                     |
      | 2            | 1                   |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1,2          | 1234    | SØKER      | 11.01.1970  |
      | 1,2          | 3456    | BARN       | 07.09.2019  |
      | 1,2          | 7890    | BARN       | 22.08.2022  |

  Scenario: Skal kunne generere begrunnelser
    Og lag personresultater for begrunnelse for behandling 1
    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 1234    | UTVIDET_BARNETRYGD                                              | 07.09.2022 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 07.09.2019 | 06.09.2037 | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                                   | 07.09.2022 | 15.05.2023 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD                | 07.09.2019 |            | Oppfylt  |
      | 7890    | UNDER_18_ÅR                                                     | 22.08.2022 | 23.08.2040 | Oppfylt  |
      | 7890    | BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 22.08.2022 |            | Oppfylt  |

    Og lag personresultater for begrunnelse for behandling 2
    Og legg til nye vilkårresultater for begrunnelse for behandling 2
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 11.01.1970 |            | Oppfylt  |
      | 1234    | UTVIDET_BARNETRYGD                               | 07.09.2022 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                      | 07.09.2019 | 06.09.2037 | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 07.09.2022 | 15.05.2023 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 07.09.2019 |            | Oppfylt  |
      | 7890    | UNDER_18_ÅR                                      | 22.08.2022 | 23.08.2040 | Oppfylt  |
      | 7890    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 22.08.2022 |            | Oppfylt  |
      | 7890    | BOR_MED_SØKER                                    | 22.08.2022 | 15.08.2023 | Oppfylt  |

  # 2234504740524 er 07.09.2019
  # 2511315421206 er 22.08.2022
  # 2696267416094 er søker
    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.10.2022 | 28.02.2023 | 1676  | 1            |
      | 3456    | 01.03.2023 | 31.05.2023 | 1723  | 1            |
      | 7890    | 01.09.2022 | 28.02.2023 | 1676  | 1            |
      | 7890    | 01.03.2023 | 30.06.2023 | 1723  | 1            |
      | 7890    | 01.07.2023 | 31.07.2028 | 1766  | 1            |
      | 7890    | 01.08.2028 | 31.07.2040 | 1310  | 1            |
      | 1234    | 01.10.2022 | 28.02.2023 | 1054  | 1            |
      | 1234    | 01.03.2023 | 30.06.2023 | 2489  | 1            |
      | 1234    | 01.07.2023 | 31.07.2040 | 2516  | 1            |
      | 3456    | 01.10.2019 | 28.02.2023 | 1676  | 2            |
      | 3456    | 01.03.2023 | 31.05.2023 | 1723  | 2            |
      | 7890    | 01.09.2022 | 28.02.2023 | 1676  | 2            |
      | 7890    | 01.03.2023 | 30.06.2023 | 1723  | 2            |
      | 7890    | 01.07.2023 | 31.08.2023 | 1766  | 2            |
      | 1234    | 01.10.2022 | 28.02.2023 | 1054  | 2            |
      | 1234    | 01.03.2023 | 30.06.2023 | 2489  | 2            |
      | 1234    | 01.07.2023 | 31.08.2023 | 2516  | 2            |
    
    Når begrunnelsetekster genereres for behandling 2

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser | Ekskluderte Begrunnelser |
      | 01.05.2017 | 31.03.2023 | UTBETALING         |                         |                          |
      | 01.04.2023 | 31.03.2024 | UTBETALING         | REDUKSJON_UNDER_6_ÅR    |                          |
      | 01.04.2024 |            | OPPHØR             | REDUKSJON_BARN_DØD      |                          |
