# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder med mor og et barn

  Bakgrunn:
    Gitt følgende vedtak
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | PersonId | Persontype | Fødselsdato |
      | 1            | 1234     | SØKER      | 11.01.1970  |
      | 1            | 3456     | BARN       | 13.04.2020  |

  Scenario: Skal lage vedtaksperioder med begrunnelser for mor med et barn - normaltilfelle
    Og lag personresultater for behandling 1
    Og med overstyring av vilkår for behandling 1
      | PersonId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234     | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456     | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456     | BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Oppfylt for   |
      | 01.02.1970 | 30.04.2020 | Utbetaling         | Kun søker     |
      | 01.05.2020 | 31.03.2038 | Utbetaling         | Barn og søker |
      | 01.04.2038 |            | Utbetaling         | Kun søker     |


  Scenario: Skal lage vedtaksperioder med begrunnelser for mor med et barn - barn flytter til søker etter 1 år

    Og lag personresultater for behandling 1
    Og med overstyring av vilkår for behandling 1
      | PersonId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234     | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 11.01.1970 |            | Oppfylt  |
      | 3456     | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456     | BOR_MED_SØKER                                    | 20.08.2021 |            | Oppfylt  |
      | 3456     | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Oppfylt for                                       |
      | 01.02.1970 | 31.08.2021 | Utbetaling         | Kun søker                                         |
      | 01.02.1970 | 30.04.2020 | OPPHØR             | Skjønner ikke helt denne?                         |
      | 01.05.2020 | 31.08.2021 | OPPHØR             | Barn har vilkår som er oppfylt her, men ikke alle |
      | 01.09.2021 | 31.03.2038 | Utbetaling         | Barn og søker                                     |
      | 01.04.2038 |            | Utbetaling         | Kun søker                                         |


  Scenario: Skal lage vedtaksperioder med begrunnelser for mor når barnet flytter ut

    Og lag personresultater for behandling 1
    Og med overstyring av vilkår for behandling 1
      | PersonId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234     | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 11.01.1970 |            | Oppfylt  |
      | 3456     | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456     | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |
      | 3456     | BOR_MED_SØKER                                    | 13.04.2020 | 21.07.2029 | Oppfylt  |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Oppfylt for                                                     |
      | 01.02.1970 | 30.04.2020 | Utbetaling         | Kun søker                                                       |
      | 01.05.2020 | 31.07.2029 | Utbetaling         | Barn og søker                                                   |
      | 01.08.2029 |            | Utbetaling         | Kun søker                                                       |
      | 01.08.2029 | 31.03.2038 | Opphør             | Barn har oppfylte vilkår, men ett som ikke oppfylles i perioden |
      | 01.04.2038 |            | Opphør             | Barns vilkår opphøres                                           |


  Scenario: Skal lage vedtaksperioder med begrunnelser for mor når barnet flytter ut og inn igjen

    Og lag personresultater for behandling 1
    Og med overstyring av vilkår for behandling 1
      | PersonId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234     | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 11.01.1970 |            | Oppfylt  |
      | 3456     | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456     | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |
      | 3456     | BOR_MED_SØKER                                    | 13.04.2020 | 21.07.2029 | Oppfylt  |

    Og legg til nye vilkårresultater for behandling 1
      | PersonId | Vilkår        | Fra dato   | Til dato   | Resultat     |
      | 3456     | BOR_MED_SØKER | 22.07.2029 | 16.05.2030 | Ikke_oppfylt |
      | 3456     | BOR_MED_SØKER | 17.05.2030 |            | Oppfylt      |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Oppfylt for   |
      | 01.02.1970 | 30.04.2020 | Utbetaling         | Kun søker     |
      | 01.05.2020 | 31.07.2029 | Utbetaling         | Barn og søker |
      | 01.08.2029 | 31.05.2030 | Utbetaling         | Kun søker     |
      | 01.06.2030 | 31.03.2038 | Utbetaling         | Barn og søker |
      | 01.04.2038 |            | Utbetaling         | Kun søker     |




