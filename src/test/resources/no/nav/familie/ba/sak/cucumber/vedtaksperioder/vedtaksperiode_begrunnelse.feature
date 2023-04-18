# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder med begrunnelser

  Scenario: Skal lage vedtaksperioder med begrunnelser for mor med et barn - normaltilfelle

    Gitt følgende vedtak
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | PersonId | Persontype | Fødselsdato |
      | 1            | 1234     | SØKER      | 11.01.1970  |
      | 1            | 3456     | BARN       | 13.04.2020  |

    Og lag personresultater for behandling 1 med overstyringer
      | PersonId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234     | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | OPPFYLT  |
      | 3456     | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | OPPFYLT  |
      | 3456     | BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | OPPFYLT  |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Oppfylt for                         |
      | 01.02.1970 | 30.04.2020 | Utbetaling         | SØKER - bør ikke være utbetaling??? |
      | 01.05.2020 | 31.03.2038 | Utbetaling         | BARN + SØKER                        |
      | 01.04.2038 |            | Utbetaling         | SØKER - bør ikke være utbetaling??? |


  Scenario: Skal lage vedtaksperioder med begrunnelser for mor med et barn - barn flytter til søker etter 1 år

    Gitt følgende vedtak
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | PersonId | Persontype | Fødselsdato |
      | 1            | 1234     | SØKER      | 11.01.1970  |
      | 1            | 3456     | BARN       | 13.04.2020  |

    Og lag personresultater for behandling 1 med overstyringer
      | PersonId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234     | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 11.01.1970 |            | OPPFYLT  |
      | 3456     | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | OPPFYLT  |
      | 3456     | BOR_MED_SØKER                                    | 20.08.2021 |            | OPPFYLT  |
      | 3456     | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | OPPFYLT  |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Oppfylt for                         |
      | 01.02.1970 | 31.08.2021 | Utbetaling         | SØKER - bør ikke være utbetaling??? |
      | 01.02.1970 | 30.04.2020 | OPPHØR             | SØKER - bør ikke være opphør???     |
      | 01.05.2020 | 31.08.2021 | OPPHØR             | SØKER - bør ikke være opphør???     |
      | 01.09.2021 | 31.03.2038 | Utbetaling         | BARN + SØKER                        |
      | 01.04.2038 |            | Utbetaling         | SØKER - bør ikke være utbetaling??? |


  Scenario: Skal lage vedtaksperioder med begrunnelser for mor, og to barn - nytt barn kommer til

    Gitt følgende vedtak
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | PersonId | Persontype | Fødselsdato |
      | 1            | 1234     | SØKER      | 11.01.1970  |
      | 1            | 3456     | BARN       | 13.04.2020  |
      | 1            | 7890     | BARN       | 07.12.2022  |

    Og lag personresultater for behandling 1 med overstyringer
      | PersonId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234     | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | OPPFYLT  |
      | 3456     | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | OPPFYLT  |
      | 3456     | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2020 |            | OPPFYLT  |
      | 7890     | UNDER_18_ÅR                                                     | 07.12.2022 | 06.12.2040 | OPPFYLT  |
      | 7890     | BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 07.12.2022 |            | OPPFYLT  |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Oppfylt for                         |
      | 01.02.1970 | 30.04.2020 | Utbetaling         | SØKER - bør ikke være utbetaling??? |
      | 01.05.2020 | 31.12.2022 | Utbetaling         | BARN1 + SØKER                       |
      | 01.01.2023 | 31.03.2038 | Utbetaling         | BARN1 + BARN2 + SØKER               |
      | 01.04.2038 | 30.11.2040 | Utbetaling         | BARN2 + SØKER                       |
      | 01.12.2040 |            | Utbetaling         | SØKER - bør ikke være utbetaling??? |
      | 01.02.1970 | 31.12.2022 | OPPHØR             | SØKER - bør ikke være utbetaling??? |
      | 01.04.2038 |            | OPPHØR             | SØKER - bør ikke være utbetaling??? |




