# language: no
# encoding: UTF-8

Egenskap: Innvilget delt bosted og reduksjon pga barn 6 år i samme periode

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | FORTSATT_INNVILGET  | OMREGNING_6ÅR    | Ja                        | NASJONAL            |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | NYE_OPPLYSNINGER | Nei                       | NASJONAL            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 28.05.1986  |
      | 1            | 2       | BARN       | 23.02.2011  |
      | 1            | 3       | BARN       | 07.05.2015  |
      | 1            | 4       | BARN       | 03.12.2017  |
      | 2            | 1       | SØKER      | 28.05.1986  |
      | 2            | 2       | BARN       | 23.02.2011  |
      | 2            | 3       | BARN       | 07.05.2015  |
      | 2            | 4       | BARN       | 03.12.2017  |

  Scenario: 2 barn innvilget delt bosted og ett av barna blir 6 år i samme periode
    Og dagens dato er 03.01.2024
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                  | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | GIFT_PARTNERSKAP                            |                  | 23.02.2011 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                                 |                  | 23.02.2011 | 22.02.2029 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                  | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                               | DELT_BOSTED      | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR                                 |                  | 07.05.2015 | 06.05.2033 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP                            |                  | 07.05.2015 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 4       | GIFT_PARTNERSKAP                            |                  | 03.12.2017 |            | OPPFYLT  | Nei                  |                      |                  |
      | 4       | UNDER_18_ÅR                                 |                  | 03.12.2017 | 02.12.2035 | OPPFYLT  | Nei                  |                      |                  |
      | 4       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER |                  | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | UNDER_18_ÅR                   |                  | 23.02.2011 | 22.02.2029 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP              |                  | 23.02.2011 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED      | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | GIFT_PARTNERSKAP              |                  | 07.05.2015 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR                   |                  | 07.05.2015 | 06.05.2033 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                 |                  | 31.12.2021 | 04.11.2023 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                 | DELT_BOSTED      | 05.11.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 4       | UNDER_18_ÅR                   |                  | 03.12.2017 | 02.12.2035 | OPPFYLT  | Nei                  |                      |                  |
      | 4       | GIFT_PARTNERSKAP              |                  | 03.12.2017 |            | OPPFYLT  | Nei                  |                      |                  |
      | 4       | BOR_MED_SØKER                 |                  | 31.12.2021 | 14.11.2023 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BOR_MED_SØKER                 | DELT_BOSTED      | 15.11.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.01.2022 | 28.02.2023 | 527   | ORDINÆR_BARNETRYGD | 50      | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 542   | ORDINÆR_BARNETRYGD | 50      | 1083 |
      | 2       | 1            | 01.07.2023 | 31.01.2029 | 655   | ORDINÆR_BARNETRYGD | 50      | 1310 |
      | 3       | 1            | 01.01.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 1            | 01.07.2023 | 30.04.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 4       | 1            | 01.01.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 4       | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 4       | 1            | 01.07.2023 | 30.11.2023 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 1            | 01.12.2023 | 30.11.2035 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

      | 2       | 2            | 01.01.2022 | 28.02.2023 | 527   | ORDINÆR_BARNETRYGD | 50      | 1054 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 542   | ORDINÆR_BARNETRYGD | 50      | 1083 |
      | 2       | 2            | 01.07.2023 | 31.01.2029 | 655   | ORDINÆR_BARNETRYGD | 50      | 1310 |
      | 3       | 2            | 01.01.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 2            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 2            | 01.01.2024 | 30.04.2033 | 655   | ORDINÆR_BARNETRYGD | 50      | 1310 |
      | 4       | 2            | 01.01.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 4       | 2            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 4       | 2            | 01.07.2023 | 30.11.2023 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 2            | 01.12.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 4       | 2            | 01.01.2024 | 30.11.2035 | 655   | ORDINÆR_BARNETRYGD | 50      | 1310 |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak       | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 3, 4    | 2            | 01.12.2023 | 31.12.2023 | DELT_BOSTED | 100     | 06.12.2023       | 2023-11-05                  |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                 | Ugyldige begrunnelser |
      | 01.12.2023 | 31.12.2023 | UTBETALING         |                                | INNVILGET_AVTALE_DELT_BOSTED_FÅR_FRA_AVTALETIDSPUNKT |                       |
      | 01.01.2024 | 31.01.2029 | UTBETALING         |                                |                                                      |                       |
      | 01.02.2029 | 30.04.2033 | UTBETALING         |                                |                                                      |                       |
      | 01.05.2033 | 30.11.2035 | UTBETALING         |                                |                                                      |                       |
      | 01.12.2035 |            | OPPHØR             |                                |                                                      |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                                 | Eøsbegrunnelser | Fritekster |
      | 01.12.2023 | 31.12.2023 | INNVILGET_AVTALE_DELT_BOSTED_FÅR_FRA_AVTALETIDSPUNKT |                 |            |
      | 01.01.2024 | 31.01.2029 |                                                      |                 |            |
      | 01.02.2029 | 30.04.2033 |                                                      |                 |            |
      | 01.05.2033 | 30.11.2035 |                                                      |                 |            |
      | 01.12.2035 |            |                                                      |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.12.2023 til 31.12.2023
      | Begrunnelse                                          | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | INNVILGET_AVTALE_DELT_BOSTED_FÅR_FRA_AVTALETIDSPUNKT | Nei           | 07.05.15 og 03.12.17 | 2           | november 2023                        | NB      | 2 620 |                  | SØKER_HAR_IKKE_RETT     |
