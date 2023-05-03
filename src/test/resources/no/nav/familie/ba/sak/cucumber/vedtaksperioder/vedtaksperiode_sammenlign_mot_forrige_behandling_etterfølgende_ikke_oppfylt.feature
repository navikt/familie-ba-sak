# language: no
# encoding: UTF-8

Egenskap: Vedtaksperiode for behandling som opphører perioder fra forrige behanlding

  Bakgrunn:
    Gitt følgende vedtak
      | BehandlingId | ForrigeBehandlingId |
      | 1            |                     |
      | 2            | 1                   |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1,2          | 1234    | SØKER      | 11.01.1970  |
      | 1,2          | 3456    | BARN       | 13.04.2020  |

  Scenario: Skal lage vedtaksperioder for revurdering mot forrige behandling hvor gjeldende behandling har opphør av flere grunner enn forrige.
    Og lag personresultater for behandling 1
    Og med overstyring av vilkår for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 13.04.2020 | 31.12.2020 | Oppfylt  |

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår        | Fra dato   | Til dato   | Resultat     |
      | 3456    | BOR_MED_SØKER | 01.01.2021 | 31.12.2021 | ikke_oppfylt |
      | 3456    | BOR_MED_SØKER | 01.01.2022 |            | Oppfylt      |

    Og lag personresultater for behandling 2
    Og med overstyring av vilkår for behandling 2
      | AktørId | Vilkår                           | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD   | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                      | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |
      | 3456    | BOSATT_I_RIKET                   | 13.04.2020 | 15.07.2021 | Oppfylt  |
      | 3456    | BOR_MED_SØKER                    | 13.04.2020 | 31.12.2020 | Oppfylt  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår         | Fra dato   | Til dato   | Resultat     |
      | 3456    | BOR_MED_SØKER  | 01.01.2021 | 31.12.2021 | ikke_oppfylt |
      | 3456    | BOSATT_I_RIKET | 16.07.2021 | 31.12.2021 | ikke_oppfylt |
      | 3456    | BOSATT_I_RIKET | 01.01.2022 |            | Oppfylt      |
      | 3456    | BOR_MED_SØKER  | 01.01.2022 |            | Oppfylt      |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.12.2020 | 1354  | 1            |
      | 3456    | 01.02.2022 | 31.03.2038 | 1354  | 1            |
      | 3456    | 01.05.2020 | 31.12.2020 | 1354  | 2            |
      | 3456    | 01.02.2022 | 31.03.2038 | 1354  | 2            |

    Når vedtaksperioder med begrunnelser genereres for behandling 2

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar     |
      | 01.05.2020 | 31.12.2020 | Utbetaling         |               |
      | 01.01.2021 | 31.07.2021 | Opphør             | Kun søker     |
      | 01.08.2021 | 31.01.2022 | Opphør             | Kun søker     |
      | 01.02.2022 | 31.03.2038 | Utbetaling         | Barn og søker |
      | 01.04.2038 |            | Opphør             | Kun søker     |

