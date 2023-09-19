# language: no
# encoding: UTF-8

Egenskap: Reduksjon forrige periode

  Bakgrunn:
    Gitt følgende vedtak
      | BehandlingId | ForrigeBehandlingId |
      | 100172051    |                     |
      | 100172052    | 100172051           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId       | Persontype | Fødselsdato |
      | 100172051    | 2922823262515 | BARN       | 06.10.2007  |
      | 100172051    | 2956853057896 | BARN       | 27.05.2005  |
      | 100172051    | 2724744951598 | SØKER      | 22.02.1988  |
      | 100172052    | 2922823262515 | BARN       | 06.10.2007  |
      | 100172052    | 2956853057896 | BARN       | 27.05.2005  |
      | 100172052    | 2724744951598 | SØKER      | 22.02.1988  |

  Scenario: Skal ikke splitte når det er reduksjon fra forrige periode selv om det er reduksjon fra forrige behandling
    Og følgende dagens dato 18.09.2023
    Og lag personresultater for behandling 100172051
    Og lag personresultater for behandling 100172052

    Og legg til nye vilkårresultater for behandling 100172051
      | AktørId       | Vilkår                                         | Fra dato   | Til dato   | Resultat |
      | 2724744951598 | BOSATT_I_RIKET,LOVLIG_OPPHOLD                  | 22.02.1988 |            | OPPFYLT  |

      | 2922823262515 | GIFT_PARTNERSKAP,BOSATT_I_RIKET,LOVLIG_OPPHOLD | 06.10.2007 |            | OPPFYLT  |
      | 2922823262515 | UNDER_18_ÅR                                    | 06.10.2007 | 05.10.2025 | OPPFYLT  |
      | 2922823262515 | BOR_MED_SØKER                                  | 15.03.2022 |            | OPPFYLT  |

      | 2956853057896 | BOSATT_I_RIKET,LOVLIG_OPPHOLD,GIFT_PARTNERSKAP | 27.05.2005 |            | OPPFYLT  |
      | 2956853057896 | UNDER_18_ÅR                                    | 27.05.2005 | 26.05.2023 | OPPFYLT  |
      | 2956853057896 | BOR_MED_SØKER                                  | 15.03.2022 |            | OPPFYLT  |

    Og legg til nye vilkårresultater for behandling 100172052
      | AktørId       | Vilkår                                         | Fra dato   | Til dato   | Resultat |
      | 2922823262515 | LOVLIG_OPPHOLD,BOSATT_I_RIKET,GIFT_PARTNERSKAP | 06.10.2007 |            | OPPFYLT  |
      | 2922823262515 | UNDER_18_ÅR                                    | 06.10.2007 | 05.10.2025 | OPPFYLT  |
      | 2922823262515 | BOR_MED_SØKER                                  | 15.03.2022 |            | OPPFYLT  |

      | 2724744951598 | BOSATT_I_RIKET,LOVLIG_OPPHOLD                  | 22.02.1988 |            | OPPFYLT  |

      | 2956853057896 | GIFT_PARTNERSKAP,BOSATT_I_RIKET,LOVLIG_OPPHOLD | 27.05.2005 |            | OPPFYLT  |
      | 2956853057896 | UNDER_18_ÅR                                    | 27.05.2005 | 26.05.2023 | OPPFYLT  |
      | 2956853057896 | BOR_MED_SØKER                                  | 15.03.2022 | 15.03.2023 | OPPFYLT  |

    Og med andeler tilkjent ytelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2956853057896 | 100172051    | 01.04.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2956853057896 | 100172051    | 01.03.2023 | 30.04.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2922823262515 | 100172051    | 01.04.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2922823262515 | 100172051    | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2922823262515 | 100172051    | 01.07.2023 | 30.09.2025 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2922823262515 | 100172052    | 01.04.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2922823262515 | 100172052    | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2922823262515 | 100172052    | 01.07.2023 | 30.09.2025 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2956853057896 | 100172052    | 01.04.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2956853057896 | 100172052    | 01.03.2023 | 31.03.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |

    Når vedtaksperioder med begrunnelser genereres for behandling 100172052

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.04.2023 | 30.06.2023 | UTBETALING         |           |
      | 01.07.2023 | 30.09.2025 | UTBETALING         |           |
      | 01.10.2025 |            | OPPHØR             |           |