# language: no
# encoding: UTF-8

Egenskap: Reduksjon utvidet

  Bakgrunn:
    Gitt følgende behandling
      | BehandlingId | FagsakId  | ForrigeBehandlingId |
      | 100172601    | 200055201 |                     |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId       | Persontype | Fødselsdato |
      | 100172601    | 2430832956785 | SØKER      | 01.09.1984  |
      | 100172601    | 2251244514392 | BARN       | 02.02.2015  |

  Scenario: Skal håndere reduksjon utdvidet
    Og lag personresultater for begrunnelse for behandling 100172601

    Og legg til nye vilkårresultater for begrunnelse for behandling 100172601
      | AktørId       | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2430832956785 | BOSATT_I_RIKET,LOVLIG_OPPHOLD                  |                  | 01.09.1984 |            | OPPFYLT  | Nei                  |
      | 2430832956785 | UTVIDET_BARNETRYGD                             |                  | 15.10.2022 | 15.05.2023 | OPPFYLT  | Nei                  |

      | 2251244514392 | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 02.02.2015 |            | OPPFYLT  | Nei                  |
      | 2251244514392 | UNDER_18_ÅR                                    |                  | 02.02.2015 | 01.02.2033 | OPPFYLT  | Nei                  |
      | 2251244514392 | BOR_MED_SØKER                                  |                  | 15.02.2022 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 2251244514392 | 100172601    | 01.03.2022 | 01.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     |
      | 2251244514392 | 100172601    | 01.03.2023 | 01.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     |
      | 2251244514392 | 100172601    | 01.07.2023 | 01.01.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     |
      | 2430832956785 | 100172601    | 01.11.2022 | 01.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     |
      | 2430832956785 | 100172601    | 01.03.2023 | 01.05.2023 | 2489  | UTVIDET_BARNETRYGD | 100     |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | Fra dato | Til dato | BehandlingId | Årsak | Prosent |

    Når begrunnelsetekster genereres for behandling 100172601

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser                  | Ekskluderte Begrunnelser |
      | 01.03.2022 | 31.10.2022 | UTBETALING         |           |                                          |                          |
      | 01.11.2022 | 28.02.2023 | UTBETALING         |           |                                          |                          |
      | 01.03.2023 | 31.05.2023 | UTBETALING         |           |                                          |                          |
      | 01.06.2023 | 30.06.2023 | UTBETALING         |           | REDUKSJON_SAMBOER_IKKE_LENGER_FORSVUNNET |                          |
      | 01.07.2023 | 31.01.2033 | UTBETALING         |           |                                          |                          |
      | 01.02.2033 |            | OPPHØR             |           |                                          |                          |