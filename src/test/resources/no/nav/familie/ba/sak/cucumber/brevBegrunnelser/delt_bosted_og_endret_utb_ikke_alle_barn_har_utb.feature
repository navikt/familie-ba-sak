# language: no
# encoding: UTF-8

Egenskap: Delt bosted og endret utbetaling, ikke alle barn har utbetaling

  Scenario: Antall barn - Ett barn skal utbetales delt bosted, men ikke det andre
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | ENDRET_UTBETALING   | SATSENDRING      | Ja                        | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | INNVILGET_OG_ENDRET | SØKNAD           | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 11.05.1987  |              |
      | 1            | 3       | BARN       | 26.11.2014  |              |
      | 2            | 1       | SØKER      | 11.05.1987  |              |
      | 2            | 2       | BARN       | 16.05.2006  |              |
      | 2            | 3       | BARN       | 26.11.2014  |              |

    Og dagens dato er 05.06.2024
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | GIFT_PARTNERSKAP                            |                  | 26.11.2014 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR                                 |                  | 26.11.2014 | 25.11.2032 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER |                  | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD            |                  | 21.04.2023 |            | OPPFYLT  | Nei                  |                      |                  |

      | 2       | GIFT_PARTNERSKAP              |                  | 16.05.2006 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                   |                  | 16.05.2006 | 15.05.2024 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 19.10.2013 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED      | 01.05.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | GIFT_PARTNERSKAP              |                  | 26.11.2014 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR                   |                  | 26.11.2014 | 25.11.2032 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER                 |                  | 01.01.2022 | 30.04.2023 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                 | DELT_BOSTED      | 01.05.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak       | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 3       | 2            | 01.06.2023 | 30.04.2024 | DELT_BOSTED | 100     | 10.04.2024       | 2023-05-01                  |
      | 2       | 2            | 01.06.2023 | 29.02.2024 | DELT_BOSTED | 0       | 18.02.2024       | 2023-05-01                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3       | 1            | 01.02.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 1            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 1            | 01.01.2024 | 31.10.2032 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |

      | 1       | 2            | 01.05.2023 | 31.05.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 1       | 2            | 01.06.2023 | 30.06.2023 | 1245  | UTVIDET_BARNETRYGD | 50      | 2489 |
      | 1       | 2            | 01.07.2023 | 31.10.2032 | 1258  | UTVIDET_BARNETRYGD | 50      | 2516 |
      | 2       | 2            | 01.06.2023 | 29.02.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1083 |
      | 2       | 2            | 01.03.2024 | 30.04.2024 | 755   | ORDINÆR_BARNETRYGD | 50      | 1510 |
      | 3       | 2            | 01.02.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 2            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 2            | 01.01.2024 | 30.04.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 2            | 01.05.2024 | 31.10.2032 | 755   | ORDINÆR_BARNETRYGD | 50      | 1510 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                                                                                                         | Ugyldige begrunnelser |
      | 01.05.2023 | 31.05.2023 | UTBETALING         |                                | INNVILGET_FLYTTET_ETTER_SEPARASJON                                                                                                           |                       |
      | 01.06.2023 | 30.06.2023 | UTBETALING         |                                | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY, ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_MOTTATT_FULL_ORDINÆR_ETTERBETALT_UTVIDET_NY |                       |
      | 01.07.2023 | 31.12.2023 | UTBETALING         |                                |                                                                                                                                              |                       |
      | 01.01.2024 | 29.02.2024 | UTBETALING         |                                |                                                                                                                                              |                       |
      | 01.03.2024 | 30.04.2024 | UTBETALING         |                                |                                                                                                                                              |                       |
      | 01.05.2024 | 31.10.2032 | UTBETALING         |                                |                                                                                                                                              |                       |
      | 01.11.2032 |            | OPPHØR             |                                |                                                                                                                                              |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                                                                                                                         | Eøsbegrunnelser | Fritekster |
      | 01.05.2023 | 31.05.2023 | INNVILGET_FLYTTET_ETTER_SEPARASJON                                                                                                           |                 |            |
      | 01.06.2023 | 30.06.2023 | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY, ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_MOTTATT_FULL_ORDINÆR_ETTERBETALT_UTVIDET_NY |                 |            |
      | 01.07.2023 | 31.12.2023 |                                                                                                                                              |                 |            |
      | 01.01.2024 | 29.02.2024 |                                                                                                                                              |                 |            |
      | 01.03.2024 | 30.04.2024 |                                                                                                                                              |                 |            |
      | 01.05.2024 | 31.10.2032 |                                                                                                                                              |                 |            |
      | 01.11.2032 |            |                                                                                                                                              |                 |            |


    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.06.2023 til 30.06.2023
      | Begrunnelse                                                                       | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY                         | STANDARD |               | 16.05.06             | 1           | mai 2023                             |         | 0     | 18.02.24         | SØKER_FÅR_UTVIDET       |                             |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_MOTTATT_FULL_ORDINÆR_ETTERBETALT_UTVIDET_NY | STANDARD |               | 26.11.14             | 1           | mai 2023                             |         | 1 083 | 10.04.24         | SØKER_FÅR_UTVIDET       |                             |

  Scenario: Etterbetalt utvidet på søker,
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status    |
      | 1        | NORMAL     | AVSLUTTET |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | AVSLÅTT             | SØKNAD           | Nei                       | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | INNVILGET_OG_ENDRET | SØKNAD           | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 21.07.1972  |              |
      | 1            | 2       | BARN       | 14.12.2010  |              |
      | 2            | 1       | SØKER      | 21.07.1972  |              |
      | 2            | 2       | BARN       | 14.12.2010  |              |

    Og dagens dato er 17.06.2024
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser              | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.04.2022 |            | OPPFYLT      | Nei                  |                                   | NASJONALE_REGLER |

      | 2       | BOR_MED_SØKER                 |                  |            |            | IKKE_OPPFYLT | Ja                   | AVSLAG_MANGLER_AVTALE_DELT_BOSTED | NASJONALE_REGLER |
      | 2       | UNDER_18_ÅR                   |                  | 14.12.2010 | 13.12.2028 | OPPFYLT      | Nei                  |                                   |                  |
      | 2       | GIFT_PARTNERSKAP              |                  | 14.12.2010 |            | OPPFYLT      | Nei                  |                                   |                  |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED      | 01.04.2022 | 20.07.2022 | OPPFYLT      | Nei                  |                                   | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.04.2022 |            | OPPFYLT      | Nei                  |                                   | NASJONALE_REGLER |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                        | Utdypende vilkår         | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                          | 01.04.2022 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD            |                          | 18.12.2022 |            | OPPFYLT      | Nei                  |                      |                  |

      | 2       | UNDER_18_ÅR                   |                          | 14.12.2010 | 13.12.2028 | OPPFYLT      | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP              |                          | 14.12.2010 |            | OPPFYLT      | Nei                  |                      |                  |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                          | 01.04.2022 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED              | 01.04.2022 | 20.07.2022 | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                 | VURDERING_ANNET_GRUNNLAG | 21.07.2022 | 17.12.2022 | IKKE_OPPFYLT | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED              | 18.12.2022 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak       | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 2            | 01.01.2023 | 30.04.2023 | DELT_BOSTED | 0       | 13.04.2023       | 2022-12-18                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.05.2022 | 31.07.2022 | 527   | ORDINÆR_BARNETRYGD | 50      | 1054 |

      | 1       | 2            | 01.01.2023 | 28.02.2023 | 527   | UTVIDET_BARNETRYGD | 50      | 1054 |
      | 1       | 2            | 01.03.2023 | 30.06.2023 | 1245  | UTVIDET_BARNETRYGD | 50      | 2489 |
      | 1       | 2            | 01.07.2023 | 30.11.2028 | 1258  | UTVIDET_BARNETRYGD | 50      | 2516 |
      | 2       | 2            | 01.05.2022 | 31.07.2022 | 527   | ORDINÆR_BARNETRYGD | 50      | 1054 |
      | 2       | 2            | 01.01.2023 | 30.04.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 2       | 2            | 01.05.2023 | 30.06.2023 | 542   | ORDINÆR_BARNETRYGD | 50      | 1083 |
      | 2       | 2            | 01.07.2023 | 31.12.2023 | 655   | ORDINÆR_BARNETRYGD | 50      | 1310 |
      | 2       | 2            | 01.01.2024 | 30.11.2028 | 755   | ORDINÆR_BARNETRYGD | 50      | 1510 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                                                                                                         | Ugyldige begrunnelser |
      | 01.01.2023 | 28.02.2023 | UTBETALING         |                                | ENDRET_UTBETALING_ETTERBETALT_UTVIDET_DEL_FRA_AVTALETIDSPUNKT_SØKT_FOR_PRAKTISERT_DELT, INNVILGET_AVTALE_DELT_BOSTED_FÅR_FRA_AVTALETIDSPUNKT |                       |
      | 01.03.2023 | 30.04.2023 | UTBETALING         |                                |                                                                                                                                              |                       |
      | 01.05.2023 | 30.06.2023 | UTBETALING         |                                |                                                                                                                                              |                       |
      | 01.07.2023 | 31.12.2023 | UTBETALING         |                                |                                                                                                                                              |                       |
      | 01.01.2024 | 30.11.2028 | UTBETALING         |                                |                                                                                                                                              |                       |
      | 01.12.2028 |            | OPPHØR             |                                |                                                                                                                                              |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                                                                                                                         | Eøsbegrunnelser | Fritekster |
      | 01.01.2023 | 28.02.2023 | ENDRET_UTBETALING_ETTERBETALT_UTVIDET_DEL_FRA_AVTALETIDSPUNKT_SØKT_FOR_PRAKTISERT_DELT, INNVILGET_AVTALE_DELT_BOSTED_FÅR_FRA_AVTALETIDSPUNKT |                 |            |
      | 01.03.2023 | 30.04.2023 |                                                                                                                                              |                 |            |
      | 01.05.2023 | 30.06.2023 |                                                                                                                                              |                 |            |
      | 01.07.2023 | 31.12.2023 |                                                                                                                                              |                 |            |
      | 01.01.2024 | 30.11.2028 |                                                                                                                                              |                 |            |
      | 01.12.2028 |            |                                                                                                                                              |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.01.2023 til 28.02.2023
      | Begrunnelse                                                                            | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | ENDRET_UTBETALING_ETTERBETALT_UTVIDET_DEL_FRA_AVTALETIDSPUNKT_SØKT_FOR_PRAKTISERT_DELT | STANDARD |               | 14.12.10             | 1           | desember 2022                        |         | 0     | 13.04.23         | SØKER_FÅR_UTVIDET       |                             |
      | INNVILGET_AVTALE_DELT_BOSTED_FÅR_FRA_AVTALETIDSPUNKT                                   | STANDARD |               | 14.12.10             | 1           | desember 2022                        |         | 0     |                  | SØKER_FÅR_UTVIDET       |                             |

  Scenario: Endret utbetaling, delt bosted, søker får kun etterbetalt utvidet
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status    |
      | 1        | NORMAL     | AVSLUTTET |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | AVSLÅTT             | SØKNAD           | Nei                       | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | INNVILGET_OG_ENDRET | SØKNAD           | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 05.01.1981  |              |
      | 1            | 2       | BARN       | 26.09.2011  |              |
      | 1            | 3       | BARN       | 03.06.2014  |              |
      | 1            | 4       | BARN       | 18.12.2018  |              |
      | 2            | 1       | SØKER      | 05.01.1981  |              |
      | 2            | 2       | BARN       | 26.09.2011  |              |
      | 2            | 3       | BARN       | 03.06.2014  |              |
      | 2            | 4       | BARN       | 18.12.2018  |              |

    Og dagens dato er 19.06.2024
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser              | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 05.01.1981 |            | OPPFYLT      | Nei                  |                                   | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD            |                  | 31.08.2023 |            | OPPFYLT      | Nei                  |                                   |                  |

      | 2       | BOR_MED_SØKER                 |                  |            |            | IKKE_OPPFYLT | Ja                   | AVSLAG_MANGLER_AVTALE_DELT_BOSTED | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 26.09.2011 |            | OPPFYLT      | Nei                  |                                   | NASJONALE_REGLER |
      | 2       | UNDER_18_ÅR                   |                  | 26.09.2011 | 25.09.2029 | OPPFYLT      | Nei                  |                                   |                  |
      | 2       | GIFT_PARTNERSKAP              |                  | 26.09.2011 |            | OPPFYLT      | Nei                  |                                   |                  |

      | 3       | BOR_MED_SØKER                 |                  |            |            | IKKE_OPPFYLT | Ja                   | AVSLAG_MANGLER_AVTALE_DELT_BOSTED | NASJONALE_REGLER |
      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 03.06.2014 |            | OPPFYLT      | Nei                  |                                   | NASJONALE_REGLER |
      | 3       | UNDER_18_ÅR                   |                  | 03.06.2014 | 02.06.2032 | OPPFYLT      | Nei                  |                                   |                  |
      | 3       | GIFT_PARTNERSKAP              |                  | 03.06.2014 |            | OPPFYLT      | Nei                  |                                   |                  |

      | 4       | BOR_MED_SØKER                 |                  |            |            | IKKE_OPPFYLT | Ja                   | AVSLAG_MANGLER_AVTALE_DELT_BOSTED | NASJONALE_REGLER |
      | 4       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 18.12.2018 |            | OPPFYLT      | Nei                  |                                   | NASJONALE_REGLER |
      | 4       | UNDER_18_ÅR                   |                  | 18.12.2018 | 17.12.2036 | OPPFYLT      | Nei                  |                                   |                  |
      | 4       | GIFT_PARTNERSKAP              |                  | 18.12.2018 |            | OPPFYLT      | Nei                  |                                   |                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 05.01.1981 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD            |                  | 31.08.2023 |            | OPPFYLT  | Nei                  |                      |                  |

      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 26.09.2011 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | UNDER_18_ÅR                   |                  | 26.09.2011 | 25.09.2029 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP              |                  | 26.09.2011 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED      | 30.04.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | GIFT_PARTNERSKAP              |                  | 03.06.2014 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 03.06.2014 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | UNDER_18_ÅR                   |                  | 03.06.2014 | 02.06.2032 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER                 | DELT_BOSTED      | 30.04.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 4       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 18.12.2018 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | GIFT_PARTNERSKAP              |                  | 18.12.2018 |            | OPPFYLT  | Nei                  |                      |                  |
      | 4       | UNDER_18_ÅR                   |                  | 18.12.2018 | 17.12.2036 | OPPFYLT  | Nei                  |                      |                  |
      | 4       | BOR_MED_SØKER                 | DELT_BOSTED      | 30.04.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak       | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 4       | 2            | 01.05.2024 | 31.05.2024 | DELT_BOSTED | 0       | 15.05.2024       | 2024-04-30                  |
      | 3       | 2            | 01.05.2024 | 31.05.2024 | DELT_BOSTED | 0       | 15.05.2024       | 2024-04-30                  |
      | 2       | 2            | 01.05.2024 | 31.05.2024 | DELT_BOSTED | 0       | 15.05.2024       | 2024-04-30                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 1       | 2            | 01.05.2024 | 30.11.2036 | 1258  | UTVIDET_BARNETRYGD | 50      | 2516 |
      | 2       | 2            | 01.05.2024 | 31.05.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1510 |
      | 2       | 2            | 01.06.2024 | 31.08.2029 | 755   | ORDINÆR_BARNETRYGD | 50      | 1510 |
      | 3       | 2            | 01.05.2024 | 31.05.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1510 |
      | 3       | 2            | 01.06.2024 | 31.05.2032 | 755   | ORDINÆR_BARNETRYGD | 50      | 1510 |
      | 4       | 2            | 01.05.2024 | 31.05.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 4       | 2            | 01.06.2024 | 30.11.2024 | 883   | ORDINÆR_BARNETRYGD | 50      | 1766 |
      | 4       | 2            | 01.12.2024 | 30.11.2036 | 755   | ORDINÆR_BARNETRYGD | 50      | 1510 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                                                                                                                                    | Ugyldige begrunnelser |
      | 01.05.2024 | 31.05.2024 | UTBETALING         |                                | INNVILGET_DELT_FRA_SKRIFTLIG_AVTALE_HAR_SØKT_FOR_PRAKTISERT_DELT_BOSTED, ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_KUN_ETTERBETALT_UTVIDET_NY, INNVILGET_BOR_ALENE_MED_BARN |                       |
      | 01.06.2024 | 30.11.2024 | UTBETALING         |                                | ETTER_ENDRET_UTBETALING_HAR_AVTALE_DELT_BOSTED                                                                                                                          |                       |
      | 01.12.2024 | 31.08.2029 | UTBETALING         |                                |                                                                                                                                                                         |                       |
      | 01.09.2029 | 31.05.2032 | UTBETALING         |                                |                                                                                                                                                                         |                       |
      | 01.06.2032 | 30.11.2036 | UTBETALING         |                                |                                                                                                                                                                         |                       |
      | 01.12.2036 |            | OPPHØR             |                                |                                                                                                                                                                         |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                                                                                                                                                    | Eøsbegrunnelser | Fritekster |
      | 01.05.2024 | 31.05.2024 | INNVILGET_DELT_FRA_SKRIFTLIG_AVTALE_HAR_SØKT_FOR_PRAKTISERT_DELT_BOSTED, ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_KUN_ETTERBETALT_UTVIDET_NY, INNVILGET_BOR_ALENE_MED_BARN |                 |            |
      | 01.06.2024 | 30.11.2024 | ETTER_ENDRET_UTBETALING_HAR_AVTALE_DELT_BOSTED                                                                                                                          |                 |            |
      | 01.12.2024 | 31.08.2029 |                                                                                                                                                                         |                 |            |
      | 01.09.2029 | 31.05.2032 |                                                                                                                                                                         |                 |            |
      | 01.06.2032 | 30.11.2036 |                                                                                                                                                                         |                 |            |
      | 01.12.2036 |            |                                                                                                                                                                         |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.05.2024 til 31.05.2024
      | Begrunnelse                                                             | Type     | Gjelder søker | Barnas fødselsdatoer           | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | INNVILGET_DELT_FRA_SKRIFTLIG_AVTALE_HAR_SØKT_FOR_PRAKTISERT_DELT_BOSTED | STANDARD |               | 26.09.11, 03.06.14 og 18.12.18 | 3           | april 2024                           |         | 0     |                  | SØKER_FÅR_UTVIDET       |                             |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_KUN_ETTERBETALT_UTVIDET_NY        | STANDARD |               | 26.09.11, 03.06.14 og 18.12.18 | 3           | april 2024                           |         | 0     | 15.05.24         | SØKER_FÅR_UTVIDET       |                             |
      | INNVILGET_BOR_ALENE_MED_BARN                                            | STANDARD | Ja            | 26.09.11, 03.06.14 og 18.12.18 | 3           | april 2024                           |         | 1 258 |                  | SØKER_FÅR_UTVIDET       |                             |
