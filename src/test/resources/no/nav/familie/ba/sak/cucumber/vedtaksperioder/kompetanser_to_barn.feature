# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder med kompetanser for flere barn

  Bakgrunn:
    Gitt følgende behandlinger
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 13.04.2020  |
      | 1            | 7890    | BARN       | 07.12.2022  |

  Scenario: Skal lage vedtaksperioder for mor med to barn med kompetanser
    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |
      | 7890    | UNDER_18_ÅR                                                     | 07.12.2022 | 06.12.2040 | Oppfylt  |
      | 7890    | BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 07.12.2022 |            | Oppfylt  |

    Og med kompetanser
      | AktørId   | Fra dato   | Til dato   | Resultat              | BehandlingId |
      | 3456       | 01.05.2020 | 31.12.2022 | NORGE_ER_PRIMÆRLAND   | 1            |
      | 3456, 7890 | 01.01.2023 | 30.04.2023 | NORGE_ER_SEKUNDÆRLAND | 1            |
      | 3456, 7890 | 01.05.2023 | 31.03.2038 | NORGE_ER_PRIMÆRLAND   | 1            |
      | 7890       | 01.04.2038 | 30.11.2040 | NORGE_ER_SEKUNDÆRLAND | 1            |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2038 | 1054  | 1            |
      | 7890    | 01.01.2023 | 30.11.2040 | 1354  | 1            |


    Når vedtaksperiodene genereres for behandling 1

    Så forvent følgende vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar            |
      | 01.05.2020 | 31.12.2022 | Utbetaling         | Barn og søker        |
      | 01.01.2023 | 30.04.2023 | Utbetaling         | Barna og søker       |
      | 01.05.2023 | 31.03.2038 | Utbetaling         | Barna og søker       |
      | 01.04.2038 | 30.11.2040 | Utbetaling         | Barn og søker        |
      | 01.12.2040 |            | Opphør             | Kun søker            |
