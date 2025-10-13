# language: no
# encoding: UTF-8

Egenskap: Svalbardtillegg autovedtak

  Scenario: Skal ikke ta hensyn til perioder før cutoff datoen 01.10.2025

    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat        | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | ENDRET_UTBETALING          | SVALBARDTILLEGG  | Ja                        | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | DELVIS_INNVILGET_OG_ENDRET | SØKNAD           | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 24.08.1994  |              |
      | 1            | 2       | BARN       | 14.04.2013  |              |
      | 1            | 3       | BARN       | 18.08.2019  |              |
      | 1            | 4       | BARN       | 01.11.2021  |              |
      | 2            | 1       | SØKER      | 24.08.1994  |              |
      | 2            | 2       | BARN       | 14.04.2013  |              |
      | 2            | 3       | BARN       | 18.08.2019  |              |
      | 2            | 4       | BARN       | 01.11.2021  |              |


    Og dagens dato er 13.10.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 2            | 4       |
      | 2            | 3       |
      | 2            | 2       |
      | 2            | 1       |
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår   | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD               |                    | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET               |                    | 01.03.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET               | BOSATT_PÅ_SVALBARD | 01.09.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | UNDER_18_ÅR                  |                    | 14.04.2013 | 13.04.2031 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP             |                    | 14.04.2013 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET               |                    | 01.03.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | LOVLIG_OPPHOLD,BOR_MED_SØKER |                    | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET               | BOSATT_PÅ_SVALBARD | 01.09.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | GIFT_PARTNERSKAP             |                    | 18.08.2019 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR                  |                    | 18.08.2019 | 17.08.2037 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | LOVLIG_OPPHOLD               |                    | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOSATT_I_RIKET               |                    | 01.03.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                |                    | 01.03.2022 | 09.05.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                | DELT_BOSTED        | 10.05.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOSATT_I_RIKET               | BOSATT_PÅ_SVALBARD | 01.09.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 4       | GIFT_PARTNERSKAP             |                    | 01.11.2021 |            | OPPFYLT  | Nei                  |                      |                  |
      | 4       | UNDER_18_ÅR                  |                    | 01.11.2021 | 31.10.2039 | OPPFYLT  | Nei                  |                      |                  |
      | 4       | LOVLIG_OPPHOLD               |                    | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BOSATT_I_RIKET               |                    | 01.03.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BOR_MED_SØKER                |                    | 01.03.2022 | 09.05.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BOR_MED_SØKER                | DELT_BOSTED        | 10.05.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BOSATT_I_RIKET               | BOSATT_PÅ_SVALBARD | 01.09.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår   | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD               |                    | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET               |                    | 01.03.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD           |                    | 10.05.2025 |            | OPPFYLT  | Nei                  |                      |                  |
      | 1       | BOSATT_I_RIKET               | BOSATT_PÅ_SVALBARD | 01.09.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | GIFT_PARTNERSKAP             |                    | 14.04.2013 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                  |                    | 14.04.2013 | 13.04.2031 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET               |                    | 01.03.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER,LOVLIG_OPPHOLD |                    | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET               | BOSATT_PÅ_SVALBARD | 01.09.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | GIFT_PARTNERSKAP             |                    | 18.08.2019 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR                  |                    | 18.08.2019 | 17.08.2037 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | LOVLIG_OPPHOLD               |                    | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOSATT_I_RIKET               |                    | 01.03.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                |                    | 01.03.2022 | 09.05.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                | DELT_BOSTED        | 10.05.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOSATT_I_RIKET               | BOSATT_PÅ_SVALBARD | 01.09.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 4       | GIFT_PARTNERSKAP             |                    | 01.11.2021 |            | OPPFYLT  | Nei                  |                      |                  |
      | 4       | UNDER_18_ÅR                  |                    | 01.11.2021 | 31.10.2039 | OPPFYLT  | Nei                  |                      |                  |
      | 4       | LOVLIG_OPPHOLD               |                    | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BOSATT_I_RIKET               |                    | 01.03.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BOR_MED_SØKER                |                    | 01.03.2022 | 09.05.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BOR_MED_SØKER                | DELT_BOSTED        | 10.05.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BOSATT_I_RIKET               | BOSATT_PÅ_SVALBARD | 01.09.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 3,4     | 1            | 01.06.2025 | 31.08.2025 | DELT_BOSTED        | 100     | 04.08.2025       | 2025-05-10                  |
      | 3,4     | 2            | 01.06.2025 | 31.08.2025 | DELT_BOSTED        | 100     | 04.08.2025       | 2025-05-10                  |
      | 1       | 2            | 01.06.2025 | 30.06.2025 | ETTERBETALING_3MND | 0       | 08.10.2025       |                             |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.04.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 1            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 1            | 01.09.2024 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.05.2025 | 31.03.2031 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 2       | 1            | 01.10.2025 | 31.03.2031 | 500   | SVALBARDTILLEGG   | 100     | 500  |
      | 3       | 1            | 01.04.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 3       | 1            | 01.07.2023 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.05.2025 | 31.05.2025 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 1            | 01.06.2025 | 31.08.2025 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 1            | 01.09.2025 | 31.07.2037 | 984   | ORDINÆR_BARNETRYGD | 50      | 1968 |
      | 3       | 1            | 01.10.2025 | 31.07.2037 | 250   | SVALBARDTILLEGG   | 50      | 500  |
      | 4       | 1            | 01.04.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 4       | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 4       | 1            | 01.07.2023 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 1            | 01.05.2025 | 31.05.2025 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 4       | 1            | 01.06.2025 | 31.08.2025 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 4       | 1            | 01.09.2025 | 31.10.2039 | 984   | ORDINÆR_BARNETRYGD | 50      | 1968 |
      | 4       | 1            | 01.10.2025 | 31.10.2039 | 250   | SVALBARDTILLEGG   | 50      | 500  |

      | 1       | 2            | 01.06.2025 | 30.06.2025 | 0     | UTVIDET_BARNETRYGD | 0       | 2516 |
      | 1       | 2            | 01.07.2025 | 31.03.2031 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 2            | 01.04.2031 | 31.10.2039 | 1258  | UTVIDET_BARNETRYGD | 50      | 2516 |
      | 2       | 2            | 01.04.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 2            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 2            | 01.09.2024 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 2            | 01.05.2025 | 31.03.2031 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 2       | 2            | 01.10.2025 | 31.03.2031 | 500   | SVALBARDTILLEGG   | 100     | 500  |
      | 3       | 2            | 01.04.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 3       | 2            | 01.07.2023 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 2            | 01.05.2025 | 31.05.2025 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 2            | 01.06.2025 | 31.08.2025 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 2            | 01.09.2025 | 31.07.2037 | 984   | ORDINÆR_BARNETRYGD | 50      | 1968 |
      | 3       | 2            | 01.10.2025 | 31.07.2037 | 250   | SVALBARDTILLEGG   | 50      | 500  |
      | 4       | 2            | 01.04.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 4       | 2            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 4       | 2            | 01.07.2023 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 2            | 01.05.2025 | 31.05.2025 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 4       | 2            | 01.06.2025 | 31.08.2025 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 4       | 2            | 01.09.2025 | 31.10.2039 | 984   | ORDINÆR_BARNETRYGD | 50      | 1968 |
      | 4       | 2            | 01.10.2025 | 31.10.2039 | 250   | SVALBARDTILLEGG   | 50      | 500  |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser       | Ugyldige begrunnelser |
      | 01.10.2025 | 31.03.2031 | UTBETALING         |                                | INNVILGET_SVALBARDTILLEGG |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser       | Eøsbegrunnelser | Fritekster |
      | 01.10.2025 | 31.03.2031 | INNVILGET_SVALBARDTILLEGG |                 |            |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.10.2025 til 31.03.2031
      | Begrunnelse                | Type     | Gjelder søker | Barnas fødselsdatoer           | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | INNVILGET_SVALBARDTILLEGG | STANDARD | JA            | 14.04.13, 18.08.19 og 01.11.21 | 3           | september 2025                       |         | 7 452 |                  | SØKER_FÅR_UTVIDET       |                             |
