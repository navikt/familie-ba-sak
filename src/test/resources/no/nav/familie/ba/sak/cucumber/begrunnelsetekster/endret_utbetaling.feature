# language: no
# encoding: UTF-8

Egenskap: Begrunnelse etter endret utbetaling

  Bakgrunn:
    Gitt følgende behandling
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 13.04.2020  |

  Scenario: Begrunnelse endret utbetaling delt bosted
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2020 |            | Oppfylt  |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | Fra dato   | Til dato   | BehandlingId | Årsak       | Prosent |
      | 3456    | 01.05.2020 | 31.01.2021 | 1            | DELT_BOSTED | 0       |
      | 3456    | 01.02.2021 | 31.03.2038 | 1            | DELT_BOSTED | 100     |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId | Prosent |
      | 3456    | 01.05.2020 | 31.01.2021 | 0     | 1            | 0       |
      | 3456    | 01.02.2021 | 31.03.2038 | 1354  | 1            | 100     |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser                                                                                                             | Ekskluderte Begrunnelser                                          |
      | 01.05.2020 | 31.01.2021 | UTBETALING         | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_KUN_ETTERBETALT_UTVIDET_NY                                                                    | ENDRET_UTBETALING_SEKUNDÆR_DELT_BOSTED_FULL_UTBETALING_FØR_SØKNAD |
      | 01.02.2021 | 31.03.2038 | UTBETALING         | ENDRET_UTBETALING_SEKUNDÆR_DELT_BOSTED_FULL_UTBETALING_FØR_SØKNAD, ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_KUN_ETTERBETALT_UTVIDET_NY |                                                                   |
      | 01.04.2038 |            | OPPHØR             | OPPHØR_UNDER_18_ÅR                                                                                                                  |                                                                   |

  Scenario: Begrunnelse etter endret utbetaling ETTERBETALING_3ÅR
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2020 |            | Oppfylt  |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | Fra dato   | Til dato   | BehandlingId | Årsak             | Prosent |
      | 3456    | 01.05.2020 | 31.01.2021 | 1            | ETTERBETALING_3ÅR | 0       |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId | Prosent |
      | 3456    | 01.05.2020 | 31.01.2021 | 0     | 1            | 0       |
      | 3456    | 01.02.2021 | 31.03.2038 | 1000  | 1            | 100     |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser               | Ekskluderte Begrunnelser |
      | 01.02.2021 | 31.03.2038 | UTBETALING         | ETTER_ENDRET_UTBETALING_ETTERBETALING |                          |
      | 01.04.2038 |            | OPPHØR             | OPPHØR_UNDER_18_ÅR                    |                          |


  Scenario: Skal ikke krasje dersom siste periode er endret til null prosent

    Gitt følgende behandling
      | BehandlingId | FagsakId  | ForrigeBehandlingId |
      | 100173451    | 200055851 |                     |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId       | Persontype | Fødselsdato |
      | 100173451    | 2801053239878 | BARN       | 03.08.2017  |
      | 100173451    | 2204441081804 | SØKER      | 05.06.1988  |

    Og lag personresultater for begrunnelse for behandling 100173451

    Og legg til nye vilkårresultater for begrunnelse for behandling 100173451
      | AktørId       | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2204441081804 | BOSATT_I_RIKET,LOVLIG_OPPHOLD                  |                  | 05.06.1988 |            | OPPFYLT  | Nei                  |

      | 2801053239878 | GIFT_PARTNERSKAP,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 03.08.2017 |            | OPPFYLT  | Nei                  |
      | 2801053239878 | UNDER_18_ÅR                                    |                  | 03.08.2017 | 02.08.2035 | OPPFYLT  | Nei                  |
      | 2801053239878 | BOR_MED_SØKER                                  |                  | 19.07.2023 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 2801053239878 | 100173451    | 01.08.2023 | 31.08.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     |
      | 2801053239878 | 100173451    | 01.09.2023 | 31.07.2035 | 0     | ORDINÆR_BARNETRYGD | 0       |

    Og med endrede utbetalinger for begrunnelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Årsak          | Prosent |
      | 2801053239878 | 100173451    | 01.09.2023 | 01.07.2035 | ENDRE_MOTTAKER | 0       |

    Når begrunnelsetekster genereres for behandling 100173451

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser | Ekskluderte Begrunnelser |
      | 01.08.2023 | 31.08.2023 | UTBETALING         |           |                         |                          |
      | 01.09.2023 | 31.07.2035 | OPPHØR             |           |                         |                          |
      | 01.08.2035 |            | OPPHØR             |           |                         |                          |


