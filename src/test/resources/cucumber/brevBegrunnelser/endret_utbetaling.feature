# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelse ved endret utbetaling

  Scenario: Skal gi to brevbegrunnelse når vi har delt bosted med flere avtaletidspunkt samme måned.
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | Nei                       | NASJONAL            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 24.08.1987  |
      | 1            | 2       | BARN       | 15.06.2012  |
      | 1            | 3       | BARN       | 16.06.2016  |

    Og dagens dato er 24.10.2023
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
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

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.03.2022 | 30.04.2022 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 1            | 01.05.2022 | 31.05.2022 | 527   | UTVIDET_BARNETRYGD | 50      | 1054 |
      | 2       | 1            | 01.03.2022 | 31.05.2022 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.03.2022 | 31.05.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |

    Og med endrede utbetalinger
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

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 1 i periode 01.03.2022 til 30.04.2022
      | Begrunnelse                                             | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING | STANDARD | Nei           | 16.06.16             | 1           | februar 2022                         | NB      | 1 676 | 02.02.22         | SØKER_FÅR_UTVIDET       | 02.02.22                    |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 1 i periode 01.05.2022 til 31.05.2022
      | Begrunnelse                                             | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING | STANDARD | Nei           | 16.06.16             | 1           | april 2022                           | NB      | 1 676 | 02.02.22         | SØKER_FÅR_UTVIDET       | 02.02.22                    |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING | STANDARD | Nei           | 15.06.12             | 1           | april 2022                           | NB      | 1 054 | 04.04.22         | SØKER_FÅR_UTVIDET       | 04.04.22                    |


  Scenario: Skal flette inn to barn med endret utbetaling delt bosted 100% hvor ett av barna også har redusert sats pga 6år

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
    Og dagens dato er 18.12.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
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

    Og legg til nye vilkårresultater for behandling 2
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

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.12.2023 til 31.12.2023
      | Begrunnelse                                                         | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_FULL_UTBETALING_FØR_SOKNAD_NY | STANDARD | Nei           | 07.05.15 og 03.12.17 | 2           | november 2023                        | NB      | 2 620 | 06.12.23         |                         |                             |

  Scenario: Skal ikke flette inn barn som ikke har utbetaling pga allerede utbetalt i begrunnelse som gjelder søker
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Fagsakstatus |
      | 1        | NORMAL     | AVSLUTTET    |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | OPPHØRT             | NYE_OPPLYSNINGER | Nei                       | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | NYE_OPPLYSNINGER | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 01.01.1990  |              |
      | 1            | 2       | BARN       | 13.04.2021  |              |
      | 2            | 1       | SØKER      | 01.01.1990  |              |
      | 2            | 2       | BARN       | 13.04.2021  |              |
      | 2            | 3       | BARN       | 22.12.2022  |              |

    Og dagens dato er 22.03.2024
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD               |                  | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET               |                  | 01.03.2022 | 01.10.2022 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | UNDER_18_ÅR                  |                  | 13.04.2021 | 12.04.2039 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP             |                  | 13.04.2021 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER,LOVLIG_OPPHOLD |                  | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET               |                  | 01.03.2022 | 01.10.2022 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET                              |                  | 01.03.2022 | 01.10.2022 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | LOVLIG_OPPHOLD                              |                  | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET                              |                  | 22.09.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | GIFT_PARTNERSKAP                            |                  | 13.04.2021 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                                 |                  | 13.04.2021 | 12.04.2039 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET                              |                  | 01.03.2022 | 01.10.2022 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | LOVLIG_OPPHOLD,BOR_MED_SØKER                |                  | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET                              |                  | 22.09.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | GIFT_PARTNERSKAP                            |                  | 22.12.2022 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR                                 |                  | 22.12.2022 | 21.12.2040 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 22.09.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.04.2022 | 31.10.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |

      | 2       | 2            | 01.04.2022 | 31.10.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 2            | 01.10.2023 | 31.03.2027 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 2            | 01.04.2027 | 31.03.2039 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 2            | 01.10.2023 | 31.12.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 3       | 2            | 01.01.2024 | 30.11.2028 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 2            | 01.12.2028 | 30.11.2040 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 3       | 2            | 01.10.2023 | 31.12.2023 | ALLEREDE_UTBETALT | 0       | 14.12.2023       |                             |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                        | Ugyldige begrunnelser |
      | 01.10.2023 | 31.12.2023 | UTBETALING         |                                | INNVILGET_UTENLANDSOPPHOLD_OVER_TRE_MÅNEDER |                       |
      | 01.01.2024 | 31.03.2027 | UTBETALING         |                                | ETTER_ENDRET_UTBETALING_ENDRE_MOTTAKER      |                       |
      | 01.04.2027 | 30.11.2028 | UTBETALING         |                                |                                             |                       |
      | 01.12.2028 | 31.03.2039 | UTBETALING         |                                |                                             |                       |
      | 01.04.2039 | 30.11.2040 | UTBETALING         |                                |                                             |                       |
      | 01.12.2040 |            | OPPHØR             |                                |                                             |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                        | Eøsbegrunnelser | Fritekster |
      | 01.10.2023 | 31.12.2023 | INNVILGET_UTENLANDSOPPHOLD_OVER_TRE_MÅNEDER |                 |            |
      | 01.01.2024 | 31.03.2027 | ETTER_ENDRET_UTBETALING_ENDRE_MOTTAKER      |                 |            |
      | 01.04.2027 | 30.11.2028 |                                             |                 |            |
      | 01.12.2028 | 31.03.2039 |                                             |                 |            |
      | 01.04.2039 | 30.11.2040 |                                             |                 |            |
      | 01.12.2040 |            |                                             |                 |            |

    Så forvent følgende brevperioder for behandling 2
      | Brevperiodetype | Fra dato     | Til dato          | Beløp | Antall barn med utbetaling | Barnas fødselsdager  | Du eller institusjonen |
      | UTBETALING      | oktober 2023 | til desember 2023 | 1766  | 1                          | 13.04.21             | du                     |
      | UTBETALING      | januar 2024  | til mars 2027     | 3532  | 2                          | 13.04.21 og 22.12.22 | du                     |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.10.2023 til 31.12.2023
      | Begrunnelse                                 | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp |
      | INNVILGET_UTENLANDSOPPHOLD_OVER_TRE_MÅNEDER | STANDARD | Ja            | 13.04.21             | 1           | september 2023                       | NB      | 1 766 |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.01.2024 til 31.03.2027
      | Begrunnelse                            | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt |
      | ETTER_ENDRET_UTBETALING_ENDRE_MOTTAKER | STANDARD | Nei           | 22.12.22             | 1           | desember 2023                        | NB      | 1 766 | 14.12.23         |

  Scenario: Skal flette inn barn som ikke har utbetaling pga allerede utbetalt i begrunnelse for utvidet dersom søker bare har ett barn
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status    |
      | 1        | NORMAL     | OPPRETTET |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingsstatus |
      | 1            | 1        | DELVIS_INNVILGET    | SØKNAD           | Nei                       | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 08.10.1989  |
      | 1            | 2       | BARN       | 23.06.2015  |

    Og dagens dato er 29.07.2024
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                        | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | UTVIDET_BARNETRYGD                            | 06.06.2019 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET, LOVLIG_OPPHOLD                | 06.06.2019 |            | OPPFYLT  | Nei                  |

      | 2       | UNDER_18_ÅR                                   | 23.06.2015 | 22.06.2033 | OPPFYLT  | Nei                  |
      | 2       | GIFT_PARTNERSKAP                              | 23.06.2015 |            | OPPFYLT  | Nei                  |
      | 2       | LOVLIG_OPPHOLD, BOSATT_I_RIKET, BOR_MED_SØKER | 06.06.2019 |            | OPPFYLT  | Nei                  |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent | Søknadstidspunkt |
      | 2       | 1            | 01.07.2019 | 30.04.2024 | ALLEREDE_UTBETALT | 0       | 17.11.2019       |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.07.2019 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 1            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 1       | 1            | 01.07.2023 | 31.05.2033 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 1            | 01.07.2019 | 30.04.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 2       | 1            | 01.05.2024 | 31.05.2033 | 373   | ORDINÆR_BARNETRYGD | 100     | 1510 |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Gyldige begrunnelser                                                          |
      | 01.07.2019 | 28.02.2023 | UTBETALING         | INNVILGET_SKILT, ENDRET_UTBETALING_SELVSTENDIG_RETT_ETTERBETALING_UTVIDET_DEL |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                                                          |
      | 01.07.2019 | 28.02.2023 | INNVILGET_SKILT, ENDRET_UTBETALING_SELVSTENDIG_RETT_ETTERBETALING_UTVIDET_DEL |

    Så forvent følgende brevperioder for behandling 1
      | Brevperiodetype | Fra dato  | Til dato         | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |
      | UTBETALING      | juli 2019 | til februar 2023 | 1054  | 1                          | 23.06.15            | du                     |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 1 i periode 01.07.2019 til 28.02.2023
      | Begrunnelse                                                  | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | INNVILGET_SKILT                                              | STANDARD | Ja            | 23.06.15             | 1           | juni 2019                            | 1 054 |                  | SØKER_FÅR_UTVIDET       |
      | ENDRET_UTBETALING_SELVSTENDIG_RETT_ETTERBETALING_UTVIDET_DEL | STANDARD | Nei           | 23.06.15             | 1           | juni 2019                            | 0     | 17.11.19         | SØKER_FÅR_UTVIDET       |

  Scenario: Skal kun flette inn barn som har utbetaling i begrunnelse for utvidet dersom søker har ett barn med utbetaling og ett barn som ikke har utbetaling pga allerede utbetalt
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status    |
      | 1        | NORMAL     | OPPRETTET |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingsstatus |
      | 1            | 1        | INNVILGET           | SØKNAD           | Nei                       | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 08.10.1989  |
      | 1            | 2       | BARN       | 23.06.2015  |
      | 1            | 3       | BARN       | 23.06.2016  |

    Og dagens dato er 29.07.2024
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                        | Fra dato   | Til dato   | Resultat |
      | 1       | UTVIDET_BARNETRYGD                            | 06.06.2019 |            | OPPFYLT  |
      | 1       | BOSATT_I_RIKET                                | 06.06.2019 |            | OPPFYLT  |
      | 1       | LOVLIG_OPPHOLD                                | 06.06.2019 |            | OPPFYLT  |

      | 2       | UNDER_18_ÅR                                   | 23.06.2015 | 22.06.2033 | OPPFYLT  |
      | 2       | GIFT_PARTNERSKAP                              | 23.06.2015 |            | OPPFYLT  |
      | 2       | LOVLIG_OPPHOLD, BOSATT_I_RIKET, BOR_MED_SØKER | 06.06.2019 |            | OPPFYLT  |

      | 3       | UNDER_18_ÅR                                   | 23.06.2016 | 22.06.2034 | OPPFYLT  |
      | 3       | GIFT_PARTNERSKAP                              | 23.06.2016 |            | OPPFYLT  |
      | 3       | LOVLIG_OPPHOLD, BOSATT_I_RIKET, BOR_MED_SØKER | 06.06.2019 |            | OPPFYLT  |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent | Søknadstidspunkt |
      | 2       | 1            | 01.07.2019 | 30.04.2024 | ALLEREDE_UTBETALT | 0       | 17.11.2019       |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.07.2019 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 1            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 1       | 1            | 01.07.2023 | 31.05.2033 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |

      | 2       | 1            | 01.07.2019 | 30.04.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 2       | 1            | 01.05.2024 | 31.05.2033 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |

      | 3       | 1            | 01.07.2019 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 1            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 1            | 01.01.2024 | 31.05.2034 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser |
      | 01.07.2019 | 28.02.2023 | INNVILGET_SKILT      |

    Så forvent følgende brevperioder for behandling 1
      | Brevperiodetype | Fra dato  | Til dato         | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |
      | UTBETALING      | juli 2019 | til februar 2023 | 2108  | 1                          | 23.06.16            | du                     |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 1 i periode 01.07.2019 til 28.02.2023
      | Begrunnelse     | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Beløp | Søkers rett til utvidet |
      | INNVILGET_SKILT | STANDARD | Ja            | 23.06.16             | 1           | juni 2019                            | 2 108 | SØKER_FÅR_UTVIDET       |


  Scenario: I perioder med endret utbetaling men der søker fortsatt får utbetalt så skal det være mulig å bruke standard innvilgelse begrunnelser
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat        | Behandlingsårsak         | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | FORTSATT_INNVILGET         | MÅNEDLIG_VALUTAJUSTERING | Ja                        | EØS                 | AVSLUTTET         |
      | 2            | 1        | 1                   | DELVIS_INNVILGET_OG_ENDRET | SØKNAD                   | Nei                       | EØS                 | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 01.08.1998  |              |
      | 1            | 2       | BARN       | 30.04.2019  |              |
      | 2            | 1       | SØKER      | 01.08.1998  |              |
      | 2            | 2       | BARN       | 30.04.2019  |              |


    Og dagens dato er 17.02.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 2            | 2       |
      | 2            | 1       |
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår                            | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET   | ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING | 01.08.2024 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD   |                                             | 01.08.2024 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | GIFT_PARTNERSKAP |                                             | 30.04.2019 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR      |                                             | 30.04.2019 | 29.04.2037 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS                              | 01.08.2024 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                                             | 01.08.2024 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER                    | 01.08.2024 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår             | Utdypende vilkår                            | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET     | ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING | 01.03.2024 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD     |                                             | 01.03.2024 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | UTVIDET_BARNETRYGD |                                             | 21.03.2024 |            | OPPFYLT  | Nei                  |                      |                  |

      | 2       | GIFT_PARTNERSKAP   |                                             | 30.04.2019 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR        |                                             | 30.04.2019 | 29.04.2037 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER      | BARN_BOR_I_EØS_MED_SØKER                    | 01.03.2024 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD     |                                             | 01.03.2024 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET     | BARN_BOR_I_EØS                              | 01.03.2024 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet            | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.09.2024 |            | NORGE_ER_SEKUNDÆRLAND | 1            | I_ARBEID         | MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN | PL                    | NO                             | PL                  |
      | 2       | 01.04.2024 | 31.05.2024 | NORGE_ER_SEKUNDÆRLAND | 2            | I_ARBEID         | MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN | PL                    | NO                             | PL                  |
      | 2       | 01.06.2024 | 31.07.2024 | NORGE_ER_PRIMÆRLAND   | 2            | INAKTIV          | MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN | PL                    | NO                             | PL                  |
      | 2       | 01.08.2024 |            | NORGE_ER_SEKUNDÆRLAND | 2            | I_ARBEID         | MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN | PL                    | NO                             | PL                  |

    Og med utenlandsk periodebeløp
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2       | 09.2024   |           | 1            | 800   | PLN         | MÅNEDLIG  | PL              |
      | 2       | 04.2024   | 05.2024   | 2            | 800   | PLN         | MÅNEDLIG  | PL              |
      | 2       | 08.2024   |           | 2            | 800   | PLN         | MÅNEDLIG  | PL              |

    Og med valutakurser
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2       | 01.09.2024 | 30.09.2024 | 1            | 2024-08-30     | PLN         | 2.7271239155 | AUTOMATISK     |
      | 2       | 01.10.2024 | 31.10.2024 | 1            | 2024-09-30     | PLN         | 2.7494858372 | AUTOMATISK     |
      | 2       | 01.11.2024 | 30.11.2024 | 1            | 2024-10-31     | PLN         | 2.7439781190 | AUTOMATISK     |
      | 2       | 01.12.2024 | 31.12.2024 | 1            | 2024-11-29     | PLN         | 2.7189245810 | AUTOMATISK     |
      | 2       | 01.01.2025 | 31.01.2025 | 1            | 2024-12-31     | PLN         | 2.7590643275 | AUTOMATISK     |
      | 2       | 01.02.2025 |            | 1            | 2025-01-31     | PLN         | 2.7859719915 | AUTOMATISK     |
      | 2       | 01.04.2024 | 31.05.2024 | 2            | 2024-12-31     | PLN         | 2.7590643275 | MANUELL        |
      | 2       | 01.09.2024 | 30.09.2024 | 2            | 2024-08-30     | PLN         | 2.7271239155 | AUTOMATISK     |
      | 2       | 01.10.2024 | 31.10.2024 | 2            | 2024-09-30     | PLN         | 2.7494858372 | AUTOMATISK     |
      | 2       | 01.11.2024 | 30.11.2024 | 2            | 2024-10-31     | PLN         | 2.7439781190 | AUTOMATISK     |
      | 2       | 01.12.2024 | 31.12.2024 | 2            | 2024-11-29     | PLN         | 2.7189245810 | AUTOMATISK     |
      | 2       | 01.01.2025 | 31.01.2025 | 2            | 2024-12-31     | PLN         | 2.7590643275 | AUTOMATISK     |
      | 2       | 01.02.2025 |            | 2            | 2025-01-31     | PLN         | 2.7859719915 | AUTOMATISK     |
      | 2       | 01.08.2024 | 31.08.2024 | 2            | 2024-07-31     | PLN         | 2.7541484106 | AUTOMATISK     |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 2            | 01.04.2024 | 31.08.2024 | ALLEREDE_UTBETALT | 0       | 01.08.2024       |                             |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.09.2024 | 30.09.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.10.2024 | 31.10.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.11.2024 | 30.11.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.12.2024 | 31.12.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.01.2025 | 31.01.2025 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.02.2025 | 31.03.2037 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |

      | 1       | 2            | 01.04.2024 | 31.05.2024 | 309   | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 2            | 01.06.2024 | 31.07.2024 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 2            | 01.08.2024 | 31.08.2024 | 313   | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 2            | 01.09.2024 | 30.09.2024 | 2101  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 2            | 01.10.2024 | 31.10.2024 | 2083  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 2            | 01.11.2024 | 30.11.2024 | 2087  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 2            | 01.12.2024 | 31.12.2024 | 2107  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 2            | 01.01.2025 | 31.01.2025 | 2075  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 2            | 01.02.2025 | 31.03.2037 | 2054  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 2            | 01.04.2024 | 31.05.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 2            | 01.06.2024 | 31.07.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 2            | 01.08.2024 | 31.08.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 2            | 01.09.2024 | 30.09.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 2            | 01.10.2024 | 31.10.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 2            | 01.11.2024 | 30.11.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 2            | 01.12.2024 | 31.12.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 2            | 01.01.2025 | 31.01.2025 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 2            | 01.02.2025 | 31.03.2037 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                 | Ugyldige begrunnelser |
      | 01.04.2024 | 31.05.2024 | UTBETALING         | EØS_FORORDNINGEN               | INNVILGET_SEKUNDÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE |                       |
      | 01.06.2024 | 31.07.2024 | UTBETALING         | EØS_FORORDNINGEN               | INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE   |                       |
      | 01.08.2024 | 31.08.2024 | UTBETALING         | EØS_FORORDNINGEN               | INNVILGET_SEKUNDÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE |                       |

Scenario: : Endret utbetaling begrunnelser som har tema EØS skal trekke inn kompetanse i flettefelter
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status    |
      | 1        | NORMAL     | OPPRETTET |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | EØS                 | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 03.05.1991  |              |
      | 1            | 2       | BARN       | 14.02.2021  |              |

    Og dagens dato er 27.05.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 2       |
      | 1            | 1       |
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår             | Utdypende vilkår                            | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser                           | Vurderes etter   |
      | 1       | UTVIDET_BARNETRYGD |                                             | 01.06.2022 |            | OPPFYLT      | Nei                  |                                                |                  |
      | 1       | LOVLIG_OPPHOLD     |                                             | 01.06.2022 |            | OPPFYLT      | Nei                  |                                                | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET     | ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING | 01.06.2022 | 29.12.2022 | OPPFYLT      | Nei                  |                                                | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET     | ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING | 30.12.2022 | 31.01.2023 | OPPFYLT      | Nei                  |                                                | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET     |                                             | 01.02.2023 | 01.05.2023 | IKKE_OPPFYLT | Ja                   | AVSLAG_SEKUNDÆRLAND_INGEN_AV_FORELDRENE_JOBBER | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET     | ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING | 02.05.2023 |            | OPPFYLT      | Nei                  |                                                | EØS_FORORDNINGEN |

      | 2       | GIFT_PARTNERSKAP   |                                             | 14.02.2021 |            | OPPFYLT      | Nei                  |                                                |                  |
      | 2       | UNDER_18_ÅR        |                                             | 14.02.2021 | 13.02.2039 | OPPFYLT      | Nei                  |                                                |                  |
      | 2       | BOSATT_I_RIKET     | BARN_BOR_I_EØS                              | 01.06.2022 |            | OPPFYLT      | Nei                  |                                                | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD     |                                             | 01.06.2022 |            | OPPFYLT      | Nei                  |                                                | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER      | BARN_BOR_I_EØS_MED_SØKER                    | 01.06.2022 |            | OPPFYLT      | Nei                  |                                                | EØS_FORORDNINGEN |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.06.2023 |            | NORGE_ER_PRIMÆRLAND   | 1            | INAKTIV          | ARBEIDER                  | DK                    | NO                             | DK                  |
      | 2       | 01.07.2022 | 31.12.2022 | NORGE_ER_SEKUNDÆRLAND | 1            | I_ARBEID         | ARBEIDER                  | DK                    | NO                             | DK                  |
      | 2       | 01.01.2023 | 31.01.2023 | NORGE_ER_SEKUNDÆRLAND | 1            | I_ARBEID         | INAKTIV                   | DK                    | NO                             | DK                  |

    Og med utenlandsk periodebeløp
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall   | Utbetalingsland |
      | 2       | 07.2022   | 01.2023   | 1            | 4653  | DKK         | KVARTALSVIS | DK              |

    Og med valutakurser
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2       | 01.07.2022 | 31.07.2022 | 1            | 2022-06-30     | DKK         | 1.3910769975 | AUTOMATISK     |
      | 2       | 01.08.2022 | 31.08.2022 | 1            | 2022-07-29     | DKK         | 1.3269163599 | AUTOMATISK     |
      | 2       | 01.09.2022 | 30.09.2022 | 1            | 2022-08-31     | DKK         | 1.3363811163 | AUTOMATISK     |
      | 2       | 01.10.2022 | 31.10.2022 | 1            | 2022-09-30     | DKK         | 1.4232232905 | AUTOMATISK     |
      | 2       | 01.11.2022 | 30.11.2022 | 1            | 2022-10-31     | DKK         | 1.3839664714 | AUTOMATISK     |
      | 2       | 01.12.2022 | 31.12.2022 | 1            | 2022-11-30     | DKK         | 1.3803082054 | AUTOMATISK     |
      | 2       | 01.01.2023 | 31.01.2023 | 1            | 2022-12-30     | DKK         | 1.4138102602 | AUTOMATISK     |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 1            | 01.07.2022 | 31.03.2024 | ALLEREDE_UTBETALT | 0       | 01.02.2023       |                             |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 1       | 1            | 01.07.2022 | 31.01.2023 | 0     | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 1            | 01.06.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 1       | 1            | 01.07.2023 | 31.01.2039 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 1            | 01.07.2022 | 31.07.2022 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 2       | 1            | 01.08.2022 | 31.08.2022 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 2       | 1            | 01.09.2022 | 30.09.2022 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 2       | 1            | 01.10.2022 | 31.10.2022 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 2       | 1            | 01.11.2022 | 30.11.2022 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 2       | 1            | 01.12.2022 | 31.12.2022 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 2       | 1            | 01.01.2023 | 31.01.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 2       | 1            | 01.06.2023 | 31.03.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 2       | 1            | 01.04.2024 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.05.2025 | 31.01.2039 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                         | Ugyldige begrunnelser |
      | 01.07.2022 | 31.12.2022 | UTBETALING         | EØS_FORORDNINGEN               | ENDRET_UTBETALING_ETTERBETALING_UTVIDET_EØS_INGEN_UTBETALING |                       |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser | Eøsbegrunnelser                                              | Fritekster |
      | 01.07.2022 | 31.12.2022 |                      | ENDRET_UTBETALING_ETTERBETALING_UTVIDET_EØS_INGEN_UTBETALING |            |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 1 i periode 01.07.2022 til 31.12.2022
      | Begrunnelse                                                  | Type | Barnas fødselsdatoer | Antall barn | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland | Gjelder søker |
      | ENDRET_UTBETALING_ETTERBETALING_UTVIDET_EØS_INGEN_UTBETALING | EØS  | 14.02.21             | 1           | I_ARBEID         | ARBEIDER                  | Danmark               | Norge                          | Danmark             | Nei           |