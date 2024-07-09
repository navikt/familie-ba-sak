# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder ved automatisk behandling

  Bakgrunn:

  Scenario: Skal splitte vedtaksperiode på oppstart av ny andel ved omregningsoppgave
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | ENDRET_UTBETALING   | SMÅBARNSTILLEGG  |
      | 2            | 1        | 1                   | FORTSATT_INNVILGET  | OMREGNING_6ÅR    |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 30.09.1995  |
      | 1            | 2       | BARN       | 30.11.2017  |
      | 1            | 3       | BARN       | 12.10.2021  |
      | 2            | 1       | SØKER      | 30.09.1995  |
      | 2            | 2       | BARN       | 30.11.2017  |
      | 2            | 3       | BARN       | 12.10.2021  |

    Og dagens dato er 08.11.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår            | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                             | 15.09.2023 | 15.02.2024 | OPPFYLT  | Nei                  |                      |
      | 1       | UTVIDET_BARNETRYGD            |                             | 17.11.2022 |            | OPPFYLT  | Nei                  |                      |

      | 2       | GIFT_PARTNERSKAP              |                             | 30.11.2017 |            | OPPFYLT  | Nei                  |                      |
      | 2       | UNDER_18_ÅR                   |                             | 30.11.2017 | 29.11.2035 | OPPFYLT  | Nei                  |                      |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                             | 01.04.2022 |            | OPPFYLT  | Nei                  |                      |
      | 2       | BOR_MED_SØKER                 |                             | 01.04.2022 | 16.11.2022 | OPPFYLT  | Nei                  |                      |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED_SKAL_IKKE_DELES | 17.11.2022 |            | OPPFYLT  | Nei                  |                      |

      | 3       | GIFT_PARTNERSKAP              |                             | 12.10.2021 |            | OPPFYLT  | Nei                  |                      |
      | 3       | UNDER_18_ÅR                   |                             | 12.10.2021 | 11.10.2039 | OPPFYLT  | Nei                  |                      |
      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                             | 01.04.2022 |            | OPPFYLT  | Nei                  |                      |
      | 3       | BOR_MED_SØKER                 |                             | 01.04.2022 | 16.11.2022 | OPPFYLT  | Nei                  |                      |
      | 3       | BOR_MED_SØKER                 | DELT_BOSTED_SKAL_IKKE_DELES | 17.11.2022 |            | OPPFYLT  | Nei                  |                      |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                        | Utdypende vilkår            | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                             | 15.09.2023 | 15.02.2024 | OPPFYLT  | Nei                  |                      |
      | 1       | UTVIDET_BARNETRYGD            |                             | 17.11.2022 |            | OPPFYLT  | Nei                  |                      |

      | 2       | GIFT_PARTNERSKAP              |                             | 30.11.2017 |            | OPPFYLT  | Nei                  |                      |
      | 2       | UNDER_18_ÅR                   |                             | 30.11.2017 | 29.11.2035 | OPPFYLT  | Nei                  |                      |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                             | 01.04.2022 |            | OPPFYLT  | Nei                  |                      |
      | 2       | BOR_MED_SØKER                 |                             | 01.04.2022 | 16.11.2022 | OPPFYLT  | Nei                  |                      |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED_SKAL_IKKE_DELES | 17.11.2022 |            | OPPFYLT  | Nei                  |                      |

      | 3       | GIFT_PARTNERSKAP              |                             | 12.10.2021 |            | OPPFYLT  | Nei                  |                      |
      | 3       | UNDER_18_ÅR                   |                             | 12.10.2021 | 11.10.2039 | OPPFYLT  | Nei                  |                      |
      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                             | 01.04.2022 |            | OPPFYLT  | Nei                  |                      |
      | 3       | BOR_MED_SØKER                 |                             | 01.04.2022 | 16.11.2022 | OPPFYLT  | Nei                  |                      |
      | 3       | BOR_MED_SØKER                 | DELT_BOSTED_SKAL_IKKE_DELES | 17.11.2022 |            | OPPFYLT  | Nei                  |                      |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.10.2023 | 31.10.2023 | 696   | SMÅBARNSTILLEGG    | 100     | 696  |
      | 1       | 1            | 01.12.2023 | 28.02.2024 | 696   | SMÅBARNSTILLEGG    | 100     | 696  |
      | 2       | 1            | 01.10.2023 | 31.10.2023 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.11.2023 | 28.02.2024 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 1            | 01.10.2023 | 28.02.2024 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |

      | 1       | 2            | 01.10.2023 | 31.10.2023 | 696   | SMÅBARNSTILLEGG    | 100     | 696  |
      | 1       | 2            | 01.12.2023 | 28.02.2024 | 696   | SMÅBARNSTILLEGG    | 100     | 696  |
      | 2       | 2            | 01.10.2023 | 31.10.2023 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 2            | 01.11.2023 | 28.02.2024 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 2            | 01.10.2023 | 28.02.2024 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent følgende vedtaksperioder for behandling 2
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.11.2023 | 30.11.2023 | UTBETALING         |           |
