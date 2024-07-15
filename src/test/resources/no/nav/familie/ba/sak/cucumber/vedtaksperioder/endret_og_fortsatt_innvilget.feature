# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder for endret og fortsatt innvilget

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat          | Behandlingsårsak |
      | 1            | 1        |                     | ENDRET_UTBETALING            | SATSENDRING      |
      | 2            | 1        | 1                   | ENDRET_OG_FORTSATT_INNVILGET | SØKNAD           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 05.09.1993  |
      | 1            | 2       | BARN       | 19.08.2021  |
      | 2            | 1       | SØKER      | 05.09.1993  |
      | 2            | 2       | BARN       | 19.08.2021  |

  Scenario: Skal gi riktige perioder når behandlingsresultatet er endret og fortsatt innvilget
    Og dagens dato er 16.11.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser |
      | 1       | LOVLIG_OPPHOLD,UTVIDET_BARNETRYGD,BOSATT_I_RIKET |                  | 01.04.2022 |            | OPPFYLT  | Nei                  |                      |

      | 2       | UNDER_18_ÅR                                      |                  | 19.08.2021 | 18.08.2039 | OPPFYLT  | Nei                  |                      |
      | 2       | GIFT_PARTNERSKAP                                 |                  | 19.08.2021 |            | OPPFYLT  | Nei                  |                      |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER      |                  | 01.04.2022 |            | OPPFYLT  | Nei                  |                      |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser |
      | 1       | UTVIDET_BARNETRYGD                          |                  | 01.04.2022 | 05.10.2023 | OPPFYLT  | Nei                  |                      |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                  | 01.04.2022 |            | OPPFYLT  | Nei                  |                      |

      | 2       | UNDER_18_ÅR                                 |                  | 19.08.2021 | 18.08.2039 | OPPFYLT  | Nei                  |                      |
      | 2       | GIFT_PARTNERSKAP                            |                  | 19.08.2021 |            | OPPFYLT  | Nei                  |                      |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 01.04.2022 |            | OPPFYLT  | Nei                  |                      |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.05.2022 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 1            | 01.07.2022 | 30.09.2022 | 660   | SMÅBARNSTILLEGG    | 100     | 660  |
      | 1       | 1            | 01.11.2022 | 28.02.2023 | 660   | SMÅBARNSTILLEGG    | 100     | 660  |
      | 1       | 1            | 01.03.2023 | 30.06.2023 | 678   | SMÅBARNSTILLEGG    | 100     | 678  |
      | 1       | 1            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 1       | 1            | 01.07.2023 | 31.08.2024 | 696   | SMÅBARNSTILLEGG    | 100     | 696  |
      | 1       | 1            | 01.07.2023 | 31.07.2039 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 1            | 01.05.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 2       | 1            | 01.07.2023 | 31.07.2027 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.08.2027 | 31.07.2039 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 1       | 2            | 01.05.2022 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 2            | 01.07.2022 | 30.09.2022 | 660   | SMÅBARNSTILLEGG    | 100     | 660  |
      | 1       | 2            | 01.11.2022 | 28.02.2023 | 660   | SMÅBARNSTILLEGG    | 100     | 660  |
      | 1       | 2            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 1       | 2            | 01.03.2023 | 30.06.2023 | 678   | SMÅBARNSTILLEGG    | 100     | 678  |
      | 1       | 2            | 01.07.2023 | 31.07.2023 | 696   | SMÅBARNSTILLEGG    | 100     | 696  |
      | 1       | 2            | 01.07.2023 | 31.10.2023 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 2            | 01.05.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 2       | 2            | 01.07.2023 | 31.07.2027 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 2            | 01.08.2027 | 31.07.2039 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent følgende vedtaksperioder for behandling 2
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar          |
      | 01.08.2023 | 31.10.2023 | UTBETALING         | Utvidet og ordinær |
      | 01.11.2023 | 31.07.2027 | UTBETALING         | Kun ordinær        |
      | 01.08.2027 | 31.07.2039 | UTBETALING         | Kun ordinær        |
      | 01.08.2039 |            | OPPHØR             |                    |
