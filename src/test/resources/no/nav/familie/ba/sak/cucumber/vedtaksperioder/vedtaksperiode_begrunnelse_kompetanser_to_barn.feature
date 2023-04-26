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
      | 1            | 7890     | BARN       | 07.12.2022  |

  Scenario: Skal lage vedtaksperioder med begrunnelser for mor med to barn - normaltilfelle
    Og lag personresultater for behandling 1
    Og med overstyring av vilkår for behandling 1
      | PersonId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234     | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456     | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456     | BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |
      | 7890     | UNDER_18_ÅR                                                     | 07.12.2022 | 06.12.2040 | Oppfylt  |
      | 7890     | BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 07.12.2022 |            | Oppfylt  |

    Og med kompetanser for behandling 1
      | PersonId   | Fra dato   | Til dato   | Resultat              |
      | 3456       | 01.05.2020 | 31.12.2022 | NORGE_ER_PRIMÆRLAND   |
      | 3456, 7890 | 01.01.2023 | 30.04.2023 | NORGE_ER_SEKUNDÆRLAND |
      | 3456, 7890 | 01.05.2023 | 31.03.2038 | NORGE_ER_PRIMÆRLAND   |
      | 7890       | 01.04.2038 | 30.11.2040 | NORGE_ER_SEKUNDÆRLAND |

    Og med andeler tilkjent ytelse for behandling 1
      | PersonId | Fra dato   | Til dato   | Beløp |
      | 3456     | 01.05.2020 | 31.03.2038 | 1054  |
      | 7890     | 01.01.2023 | 30.11.2040 | 1354  |


    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar            |
      | 01.05.2020 | 31.12.2022 | Utbetaling         | Barn og søker        |
      | 01.01.2023 | 30.04.2023 | Utbetaling         | Barna og søker       |
      | 01.05.2023 | 31.03.2038 | Utbetaling         | Barna og søker       |
      | 01.04.2038 | 30.11.2040 | Utbetaling         | Barn og søker        |
      | 01.12.2040 |            | Opphør             | Kun søker            |
      | 01.04.2038 |            | Opphør             | Første barn opphører |
