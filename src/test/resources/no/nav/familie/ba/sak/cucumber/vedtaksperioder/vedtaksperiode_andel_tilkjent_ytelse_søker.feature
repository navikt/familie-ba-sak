# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder med andeler tilkjent ytelse

  Bakgrunn:
    Gitt følgende vedtak
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 13.04.2020  |

  Scenario: Skal lage vedtaksperioder for mor med ett barn med andeler tilkjent ytelse
    Og lag personresultater for behandling 1
    Og med overstyring av vilkår for behandling 1
      | AktørId | Vilkår                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                     | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | BOR_MED_SØKER, GIFT_PARTNERSKAP | 13.04.2020 |            | Oppfylt  |
      | 3456    | BOSATT_I_RIKET                  | 13.04.2020 | 17.07.2021 | Oppfylt  |
      | 3456    | LOVLIG_OPPHOLD                  | 17.07.2021 |            | Oppfylt  |
      | 3456    | BOSATT_I_RIKET                  | 19.09.2023 |            | Oppfylt  |

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår         | Fra dato   | Til dato   | Resultat     |
      | 3456    | BOSATT_I_RIKET | 18.07.2021 | 18.09.2023 | ikke_oppfylt |
      | 3456    | LOVLIG_OPPHOLD | 13.04.2021 | 16.07.2021 | ikke_oppfylt |
      | 3456    | BOSATT_I_RIKET | 18.09.2023 |            | Oppfylt      |
      | 3456    | LOVLIG_OPPHOLD | 17.07.2021 |            | Oppfylt      |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar              |
      | 01.10.2023 | 31.03.2038 | Utbetaling         | Barn og søker          |
      | 01.05.2020 | 31.07.2021 | Opphør             | Mangler lovlig opphold |
      | 01.08.2021 | 30.09.2023 | Opphør             | Mangler bosatt i riket |
      | 01.04.2038 |            | Opphør             | Barn er over 18        |

