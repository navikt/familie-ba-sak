# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelse ved endret utbeetaling

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | Nei                       | NASJONAL            |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 24.08.1987  |
      | 1            | 2       | BARN       | 15.06.2012  |
      | 1            | 3       | BARN       | 16.06.2016  |

  Scenario: Skal gi to brevbegrunnelse når vi har delt bosted med flere avtaletidspunkt samme måned.
    Og følgende dagens dato 24.10.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser |
      | 1       | UTVIDET_BARNETRYGD,BOSATT_I_RIKET              |                  | 24.08.1987 |            | OPPFYLT  | Nei                  |                      |
      | 1       | LOVLIG_OPPHOLD                                 |                  | 15.02.2022 | 15.05.2022 | OPPFYLT  | Nei                  |                      |

      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,GIFT_PARTNERSKAP |                  | 15.06.2012 |            | OPPFYLT  | Nei                  |                      |
      | 2       | BOR_MED_SØKER                                  |                  | 15.06.2012 | 03.04.2022 | OPPFYLT  | Nei                  |                      |
      | 2       | UNDER_18_ÅR                                    |                  | 15.06.2012 | 14.06.2030 | OPPFYLT  | Nei                  |                      |
      | 2       | BOR_MED_SØKER                                  | DELT_BOSTED      | 04.04.2022 |            | OPPFYLT  | Nei                  |                      |

      | 3       | BOSATT_I_RIKET,GIFT_PARTNERSKAP,LOVLIG_OPPHOLD |                  | 16.06.2016 |            | OPPFYLT  | Nei                  |                      |
      | 3       | BOR_MED_SØKER                                  |                  | 16.06.2016 | 01.02.2022 | OPPFYLT  | Nei                  |                      |
      | 3       | UNDER_18_ÅR                                    |                  | 16.06.2016 | 15.06.2034 | OPPFYLT  | Nei                  |                      |
      | 3       | BOR_MED_SØKER                                  | DELT_BOSTED      | 02.02.2022 |            | OPPFYLT  | Nei                  |                      |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.03.2022 | 30.04.2022 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 1            | 01.05.2022 | 31.05.2022 | 527   | UTVIDET_BARNETRYGD | 50      | 1054 |
      | 2       | 1            | 01.03.2022 | 31.05.2022 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.03.2022 | 31.05.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak       | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 3       | 1            | 01.03.2022 | 31.05.2022 | DELT_BOSTED | 100     | 02.02.2022       | 02.02.2022                  |
      | 2       | 1            | 01.05.2022 | 31.05.2022 | DELT_BOSTED | 100     | 04.04.2022       | 04.04.2022                  |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                    | Ugyldige begrunnelser |
      | 01.03.2022 | 30.04.2022 | UTBETALING         |                                | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING |                       |
      | 01.05.2022 | 31.05.2022 | UTBETALING         |                                | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING |                       |
      | 01.06.2022 |            | OPPHØR             |                                |                                                         |                       |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                                    | Eøsbegrunnelser | Fritekster |
      | 01.03.2022 | 30.04.2022 | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING |                 |            |
      | 01.05.2022 | 31.05.2022 | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING |                 |            |
      | 01.06.2022 |            |                                                         |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.03.2022 til 30.04.2022
      | Begrunnelse                                             | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING | STANDARD | Nei           | 16.06.16             | 1           | februar 2022                         | NB      | 1 676 | 02.02.22         | SØKER_FÅR_UTVIDET       | 02.02.22                    |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.05.2022 til 31.05.2022
      | Begrunnelse                                             | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING | STANDARD | Nei           | 16.06.16             | 1           | april 2022                           | NB      | 1 676 | 02.02.22         | SØKER_FÅR_UTVIDET       | 02.02.22                    |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING | STANDARD | Nei           | 15.06.12             | 1           | april 2022                           | NB      | 1 054 | 04.04.22         | SØKER_FÅR_UTVIDET       | 04.04.22                    |


  Scenario: Skal flette inn to barn med endret utbetaling delt bosted 100% hvor ett av barna også har redusert sats pga 6år

    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | FORTSATT_INNVILGET  | OMREGNING_6ÅR    | Ja                        | NASJONAL            |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | NYE_OPPLYSNINGER | Nei                       | NASJONAL            |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 28.05.1986  |
      | 1            | 2       | BARN       | 23.02.2011  |
      | 1            | 3       | BARN       | 07.05.2015  |
      | 1            | 4       | BARN       | 03.12.2017  |
      | 2            | 1       | SØKER      | 28.05.1986  |
      | 2            | 2       | BARN       | 23.02.2011  |
      | 2            | 3       | BARN       | 07.05.2015  |
      | 2            | 4       | BARN       | 03.12.2017  |
    Og følgende dagens dato 18.12.2023
    Og lag personresultater for begrunnelse for behandling 1
    Og lag personresultater for begrunnelse for behandling 2

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | UNDER_18_ÅR                                 |                  | 23.02.2011 | 22.02.2029 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP                            |                  | 23.02.2011 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                               | DELT_BOSTED      | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR                                 |                  | 07.05.2015 | 06.05.2033 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP                            |                  | 07.05.2015 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER |                  | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 4       | UNDER_18_ÅR                                 |                  | 03.12.2017 | 02.12.2035 | OPPFYLT  | Nei                  |                      |                  |
      | 4       | GIFT_PARTNERSKAP                            |                  | 03.12.2017 |            | OPPFYLT  | Nei                  |                      |                  |
      | 4       | BOR_MED_SØKER,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og legg til nye vilkårresultater for begrunnelse for behandling 2
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | GIFT_PARTNERSKAP              |                  | 23.02.2011 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                   |                  | 23.02.2011 | 22.02.2029 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED      | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR                   |                  | 07.05.2015 | 06.05.2033 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP              |                  | 07.05.2015 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER                 |                  | 31.12.2021 | 04.11.2023 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                 | DELT_BOSTED      | 05.11.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 4       | UNDER_18_ÅR                   |                  | 03.12.2017 | 02.12.2035 | OPPFYLT  | Nei                  |                      |                  |
      | 4       | GIFT_PARTNERSKAP              |                  | 03.12.2017 |            | OPPFYLT  | Nei                  |                      |                  |
      | 4       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BOR_MED_SØKER                 |                  | 31.12.2021 | 14.11.2023 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BOR_MED_SØKER                 | DELT_BOSTED      | 15.11.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med andeler tilkjent ytelse for begrunnelse
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

    Og med endrede utbetalinger for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak       | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 4       | 2            | 01.12.2023 | 31.12.2023 | DELT_BOSTED | 100     | 06.12.2023       | 2023-11-05                  |
      | 3       | 2            | 01.12.2023 | 31.12.2023 | DELT_BOSTED | 100     | 06.12.2023       | 2023-11-05                  |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                                | Ugyldige begrunnelser |
      | 01.12.2023 | 31.12.2023 | UTBETALING         |                                | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_FULL_UTBETALING_FØR_SOKNAD_NY |                       |
      | 01.01.2024 | 31.01.2029 | UTBETALING         |                                |                                                                     |                       |
      | 01.02.2029 | 30.04.2033 | UTBETALING         |                                |                                                                     |                       |
      | 01.05.2033 | 30.11.2035 | UTBETALING         |                                |                                                                     |                       |
      | 01.12.2035 |            | OPPHØR             |                                |                                                                     |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                                                | Eøsbegrunnelser | Fritekster |
      | 01.12.2023 | 31.12.2023 | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_FULL_UTBETALING_FØR_SOKNAD_NY |                 |            |
      | 01.01.2024 | 31.01.2029 |                                                                     |                 |            |
      | 01.02.2029 | 30.04.2033 |                                                                     |                 |            |
      | 01.05.2033 | 30.11.2035 |                                                                     |                 |            |
      | 01.12.2035 |            |                                                                     |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.12.2023 til 31.12.2023
      | Begrunnelse                                                         | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_FULL_UTBETALING_FØR_SOKNAD_NY | STANDARD | Nei           | 07.05.15 og 03.12.17 | 2           | november 2023                        | NB      | 2 620 | 06.12.23         |                         |                             |