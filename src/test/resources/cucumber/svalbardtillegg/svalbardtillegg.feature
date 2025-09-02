# language: no
# encoding: UTF-8

Egenskap: Svalbardtillegg

  Scenario: Ved innvilgelse av svalbardtillegg skal bare barn som får tillegget trekkes inn i relevante begrunnelser
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status    |
      | 1        | NORMAL     | OPPRETTET |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 10.12.1982  |              |
      | 1            | 2       | BARN       | 18.03.2018  |              |
      | 1            | 3       | BARN       | 31.05.2018  |              |


    Og dagens dato er 01.09.2025

    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 3       |
      | 1            | 2       |

    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår   | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD               |                    | 10.12.1982 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET               |                    | 18.03.2018 | 12.07.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET               | BOSATT_PÅ_SVALBARD | 13.07.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | GIFT_PARTNERSKAP             |                    | 18.03.2018 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | LOVLIG_OPPHOLD,BOR_MED_SØKER |                    | 18.03.2018 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET               |                    | 18.03.2018 | 12.07.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | UNDER_18_ÅR                  |                    | 18.03.2018 | 17.03.2036 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET               |                    | 13.07.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR                  |                    | 31.05.2018 | 30.05.2036 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER,LOVLIG_OPPHOLD |                    | 31.05.2018 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOSATT_I_RIKET               |                    | 31.05.2018 | 12.07.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | GIFT_PARTNERSKAP             |                    | 31.05.2018 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOSATT_I_RIKET               | BOSATT_PÅ_SVALBARD | 13.07.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 1            | 01.04.2018 | 31.05.2018 | ETTERBETALING_3MND | 0       | 01.09.2025       |                             |
      | 3,2     | 1            | 01.06.2018 | 31.05.2025 | ETTERBETALING_3MND | 0       | 01.09.2025       |                             |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 2       | 1            | 01.04.2018 | 28.02.2019 | 0     | ORDINÆR_BARNETRYGD | 0       | 970  |
      | 2       | 1            | 01.03.2019 | 31.08.2020 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 2       | 1            | 01.09.2020 | 31.08.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1354 |
      | 2       | 1            | 01.09.2021 | 31.12.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1654 |
      | 2       | 1            | 01.01.2022 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 2       | 1            | 01.07.2023 | 29.02.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 1            | 01.03.2024 | 31.08.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1510 |
      | 2       | 1            | 01.09.2024 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 1            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 1            | 01.06.2025 | 29.02.2036 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 1            | 01.06.2018 | 28.02.2019 | 0     | ORDINÆR_BARNETRYGD | 0       | 970  |
      | 3       | 1            | 01.03.2019 | 31.08.2020 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 3       | 1            | 01.09.2020 | 31.08.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1354 |
      | 3       | 1            | 01.09.2021 | 31.12.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1654 |
      | 3       | 1            | 01.01.2022 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 3       | 1            | 01.07.2023 | 30.04.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 3       | 1            | 01.05.2024 | 31.08.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1510 |
      | 3       | 1            | 01.09.2024 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 3       | 1            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 3       | 1            | 01.06.2025 | 30.04.2036 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 1            | 01.08.2025 | 30.04.2036 | 500   | SVALBARDTILLEGG    | 100     | 500  |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser      | Ugyldige begrunnelser |
      | 01.08.2025 | 29.02.2036 | UTBETALING         |                                | INNVILGET_SVALBARDTILLEGG |                       |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser      | Eøsbegrunnelser | Fritekster |
      | 01.08.2025 | 29.02.2036 | INNVILGET_SVALBARDTILLEGG |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.08.2025 til 29.02.2036
      | Begrunnelse               | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | INNVILGET_SVALBARDTILLEGG | STANDARD | Ja            | 31.05.18             | 1           | juli 2025                            |         | 4 436 |                  |                         |                             |