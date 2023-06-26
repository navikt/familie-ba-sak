# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder med mor og to barn

  Bakgrunn:
    Gitt følgende vedtak
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 13.04.2020  |

  Scenario: Skal fjerne ikke-innvilgete perioder når det ikke kommer flere innvilgete perioder

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                           | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD   | 11.01.1970 |            | Oppfylt      |                      |
      | 3456    | BOR_MED_SØKER                    | 13.04.2020 | 08.08.2021 | Oppfylt      |                      |
      | 3456    | BOR_MED_SØKER                    | 01.09.2021 |            | ikke_oppfylt | Ja                   |
      | 3456    | LOVLIG_OPPHOLD                   | 13.04.2020 | 08.08.2021 | Oppfylt      |                      |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET | 13.04.2020 | 06.06.2021 | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                      | 13.04.2020 | 12.04.2038 | Oppfylt      |                      |


    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2038 | 1054  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar      |
      | 01.05.2020 | 30.06.2021 | Utbetaling         | Barn1 og søker |
      | 01.07.2021 |            | Opphør             | Første opphør  |









