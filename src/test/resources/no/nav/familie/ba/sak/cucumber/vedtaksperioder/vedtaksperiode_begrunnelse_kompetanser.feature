# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder med kompetanser

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

    Og med kompetanser for behandling 1
      | PersonId | Fra dato   | Til dato   | Resultat |
      | 3456     | 01.05.2020 | 30.04.2021 |NORGE_ER_PRIMÆRLAND|
      | 3456     | 01.05.2021 | 31.03.2038 |NORGE_ER_SEKUNDÆRLAND|

    Og med andeler tilkjent ytelse for behandling 1
      | PersonId | Fra dato   | Til dato   | Beløp |
      | 3456     | 01.05.2020 | 30.04.2021 | 1054  |
      | 3456     | 01.05.2021 | 31.03.2038 | 1354  |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar     |
      | 01.05.2020 | 30.04.2021 | Utbetaling         | Barn og søker |
      | 01.05.2021 | 31.03.2038 | Utbetaling         | Barn og søker |
      | 01.04.2038 |            | Opphør             | Kun søker     |
