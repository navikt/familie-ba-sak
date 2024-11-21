# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser for endret utbetaling med etterbetaling tre måneder tilbake i tid

  Scenario: Skal kunne begrunne utvidet for to barn etter endret utbetaling med etterbetaling tre måneder tilbake i tid
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat         | Behandlingsårsak |
      | 1            | 1        |                     | ENDRET_UTBETALING           | NYE_OPPLYSNINGER |
      | 2            | 1        | 1                   | DELVIS_INNVILGET_OG_OPPHØRT | SØKNAD           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 09.07.1986  |
      | 1            | 2       | BARN       | 13.02.2005  |
      | 1            | 3       | BARN       | 06.11.2010  |
      | 2            | 1       | SØKER      | 09.07.1986  |
      | 2            | 2       | BARN       | 13.02.2005  |
      | 2            | 3       | BARN       | 06.11.2010  |

    Og dagens dato er 09.10.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET                                |                  | 09.07.1986 |            | OPPFYLT  | Nei                  |

      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,GIFT_PARTNERSKAP,BOR_MED_SØKER |                  | 13.02.2005 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                                  |                  | 13.02.2005 | 12.02.2023 | OPPFYLT  | Nei                  |

      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,GIFT_PARTNERSKAP               |                  | 06.11.2010 |            | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER                                                |                  | 06.11.2010 | 14.03.2018 | OPPFYLT  | Nei                  |
      | 3       | UNDER_18_ÅR                                                  |                  | 06.11.2010 | 05.11.2028 | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER                                                | DELT_BOSTED      | 15.03.2018 |            | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET                                               |                  | 09.07.1986 | 28.02.2023 | OPPFYLT  | Nei                  |
      | 1       | LOVLIG_OPPHOLD                                               |                  | 09.07.1986 |            | OPPFYLT  | Nei                  |
      | 1       | UTVIDET_BARNETRYGD                                           |                  | 01.01.2020 | 20.06.2022 | OPPFYLT  | Nei                  |

      | 2       | UNDER_18_ÅR                                                  |                  | 13.02.2005 | 12.02.2023 | OPPFYLT  | Nei                  |
      | 2       | GIFT_PARTNERSKAP,BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 13.02.2005 |            | OPPFYLT  | Nei                  |

      | 3       | GIFT_PARTNERSKAP,BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 06.11.2010 |            | OPPFYLT  | Nei                  |
      | 3       | UNDER_18_ÅR                                                  |                  | 06.11.2010 | 05.11.2028 | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER                                                |                  | 06.11.2010 | 14.03.2018 | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER                                                | DELT_BOSTED      | 15.03.2018 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.03.2005 | 28.02.2019 | 970   | ORDINÆR_BARNETRYGD | 100     | 970  |
      | 2       | 1            | 01.03.2019 | 31.01.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |

      | 3       | 1            | 01.12.2010 | 31.03.2018 | 970   | ORDINÆR_BARNETRYGD | 100     | 970  |
      | 3       | 1            | 01.04.2018 | 28.02.2019 | 485   | ORDINÆR_BARNETRYGD | 50      | 970  |
      | 3       | 1            | 01.03.2019 | 28.02.2023 | 527   | ORDINÆR_BARNETRYGD | 50      | 1054 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 542   | ORDINÆR_BARNETRYGD | 50      | 1083 |
      | 3       | 1            | 01.07.2023 | 31.10.2028 | 655   | ORDINÆR_BARNETRYGD | 50      | 1310 |

      | 1       | 2            | 01.02.2020 | 31.05.2020 | 0     | UTVIDET_BARNETRYGD | 0       | 1054 |
      | 1       | 2            | 01.06.2020 | 30.06.2022 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |

      | 2       | 2            | 01.03.2005 | 28.02.2019 | 970   | ORDINÆR_BARNETRYGD | 100     | 970  |
      | 2       | 2            | 01.03.2019 | 31.01.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |

      | 3       | 2            | 01.12.2010 | 31.03.2018 | 970   | ORDINÆR_BARNETRYGD | 100     | 970  |
      | 3       | 2            | 01.04.2018 | 28.02.2019 | 485   | ORDINÆR_BARNETRYGD | 50      | 970  |
      | 3       | 2            | 01.03.2019 | 28.02.2023 | 527   | ORDINÆR_BARNETRYGD | 50      | 1054 |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 1       | 2            | 01.02.2020 | 31.05.2020 | ETTERBETALING_3MND | 0       | 30.06.2023       | 02.02.2020                  |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Gyldige Begrunnelser                                                                 | Ugyldige Begrunnelser                                                           |
      | 01.02.2020 | 31.05.2020 | UTBETALING         |           | ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TILBAKE_I_TID_KUN_UTVIDET_DEL_UTBETALING | ENDRET_UTBETALING_ETTERBETALING_TRE_ÅR_TILBAKE_I_TID_KUN_UTVIDET_DEL_UTBETALING |
      | 01.06.2020 | 30.06.2022 | UTBETALING         |           | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_KUN_UTVIDET_DEL                    | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_AAR_KUN_UTVIDET_DEL                   |
      | 01.07.2022 | 31.01.2023 | UTBETALING         |           |                                                                                      |                                                                                 |
      | 01.02.2023 | 28.02.2023 | UTBETALING         |           |                                                                                      |                                                                                 |
      | 01.03.2023 |            | OPPHØR             |           |                                                                                      |                                                                                 |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                                                                 | Eøsbegrunnelser | Fritekster |
      | 01.02.2020 | 31.05.2020 | ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TILBAKE_I_TID_KUN_UTVIDET_DEL_UTBETALING |                 |            |
      | 01.06.2020 | 30.06.2022 | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_KUN_UTVIDET_DEL                    |                 |            |
      | 01.07.2022 | 31.01.2023 |                                                                                      |                 |            |
      | 01.02.2023 | 28.02.2023 |                                                                                      |                 |            |
      | 01.03.2023 |            |                                                                                      |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.02.2020 til 31.05.2020
      | Begrunnelse                                                                          | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet     |
      | ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TILBAKE_I_TID_KUN_UTVIDET_DEL_UTBETALING | Ja            |                      | 0           | januar 2020                          | NB      | 1 581 | 30.06.23         | SØKER_HAR_RETT_MEN_FÅR_IKKE |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.06.2020 til 30.06.2022
      | Begrunnelse                                                       | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_KUN_UTVIDET_DEL | Ja            | 13.02.05 og 06.11.10 | 2           | mai 2020                             | NB      | 2 635 | 30.06.23         | SØKER_FÅR_UTVIDET       |

  Scenario: Etter endret utbetalingsperiode begrunnelser som gjelder søker skal inkludere barn som får utbetalt i samme periode

    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat        | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | ENDRET_UTBETALING          | SATSENDRING      | Ja                        | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | DELVIS_INNVILGET_OG_ENDRET | SØKNAD           | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 08.01.1991  |              |
      | 1            | 2       | BARN       | 30.04.2015  |              |
      | 1            | 3       | BARN       | 29.08.2018  |              |
      | 2            | 1       | SØKER      | 08.01.1991  |              |
      | 2            | 2       | BARN       | 30.04.2015  |              |
      | 2            | 3       | BARN       | 29.08.2018  |              |


    Og dagens dato er 20.11.2024
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 2            | 1       |
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | GIFT_PARTNERSKAP                            |                  | 30.04.2015 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                                 |                  | 30.04.2015 | 29.04.2033 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR                                 |                  | 29.08.2018 | 28.08.2036 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP                            |                  | 29.08.2018 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD            |                  | 25.03.2022 |            | OPPFYLT  | Nei                  |                      |                  |

      | 2       | UNDER_18_ÅR                   |                  | 30.04.2015 | 29.04.2033 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP              |                  | 30.04.2015 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                 |                  | 01.02.2022 | 08.10.2024 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED      | 09.10.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR                   |                  | 29.08.2018 | 28.08.2036 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP              |                  | 29.08.2018 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                 |                  | 01.02.2022 | 08.10.2024 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                 | DELT_BOSTED      | 09.10.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 1       | 2            | 01.04.2022 | 30.06.2024 | ETTERBETALING_3MND | 0       | 23.10.2024       |                             |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.03.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 1            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 1            | 01.09.2024 | 31.03.2033 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.03.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 3       | 1            | 01.07.2023 | 31.07.2024 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.08.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 1            | 01.09.2024 | 31.07.2036 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |

      | 1       | 2            | 01.04.2022 | 30.06.2024 | 0     | UTVIDET_BARNETRYGD | 0       | 1054 |
      | 1       | 2            | 01.07.2024 | 31.10.2024 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 2            | 01.11.2024 | 31.07.2036 | 1258  | UTVIDET_BARNETRYGD | 50      | 2516 |
      | 2       | 2            | 01.03.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 2            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 2            | 01.09.2024 | 31.10.2024 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 2            | 01.11.2024 | 31.03.2033 | 883   | ORDINÆR_BARNETRYGD | 50      | 1766 |
      | 3       | 2            | 01.03.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 3       | 2            | 01.07.2023 | 31.07.2024 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 2            | 01.08.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 2            | 01.09.2024 | 31.10.2024 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 2            | 01.11.2024 | 31.07.2036 | 883   | ORDINÆR_BARNETRYGD | 50      | 1766 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                                         | Ugyldige begrunnelser |
      | 01.04.2022 | 28.02.2023 | UTBETALING         |                                |                                                                              |                       |
      | 01.03.2023 | 30.06.2023 | UTBETALING         |                                |                                                                              |                       |
      | 01.07.2023 | 31.12.2023 | UTBETALING         |                                |                                                                              |                       |
      | 01.01.2024 | 30.06.2024 | UTBETALING         |                                |                                                                              |                       |
      | 01.07.2024 | 31.07.2024 | UTBETALING         |                                | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_FOR_PRAKTISERT_DELT_BOSTED |                       |
      | 01.08.2024 | 31.08.2024 | UTBETALING         |                                |                                                                              |                       |
      | 01.09.2024 | 31.10.2024 | UTBETALING         |                                |                                                                              |                       |
      | 01.11.2024 | 31.03.2033 | UTBETALING         |                                |                                                                              |                       |
      | 01.04.2033 | 31.07.2036 | UTBETALING         |                                |                                                                              |                       |
      | 01.08.2036 |            | OPPHØR             |                                |                                                                              |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                                                         | Eøsbegrunnelser | Fritekster |
      | 01.04.2022 | 28.02.2023 |                                                                              |                 |            |
      | 01.03.2023 | 30.06.2023 |                                                                              |                 |            |
      | 01.07.2023 | 31.12.2023 |                                                                              |                 |            |
      | 01.01.2024 | 30.06.2024 |                                                                              |                 |            |
      | 01.07.2024 | 31.07.2024 | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_FOR_PRAKTISERT_DELT_BOSTED |                 |            |
      | 01.08.2024 | 31.08.2024 |                                                                              |                 |            |
      | 01.09.2024 | 31.10.2024 |                                                                              |                 |            |
      | 01.11.2024 | 31.03.2033 |                                                                              |                 |            |
      | 01.04.2033 | 31.07.2036 |                                                                              |                 |            |
      | 01.08.2036 |            |                                                                              |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.07.2024 til 31.07.2024
      | Begrunnelse                                                                  | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_FOR_PRAKTISERT_DELT_BOSTED | STANDARD | Ja            | 30.04.15 og 29.08.18 | 2           | juni 2024                            |         | 5 792 | 23.10.24         | SØKER_FÅR_UTVIDET       |                             |