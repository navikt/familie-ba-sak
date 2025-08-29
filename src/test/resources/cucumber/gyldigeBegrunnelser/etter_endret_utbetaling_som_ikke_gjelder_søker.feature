# language: no
# encoding: UTF-8

Egenskap: Etter endret utbetaling som ikke gjelder søker

  Scenario: Etter endret utbetaling begrunnelse ikke gjelder søker og ikke er allerede utbetalt skal bare ta med barn som får utbetalt

    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status    |
      | 1        | NORMAL     | OPPRETTET |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 03.12.1986  |              |
      | 1            | 2       | BARN       | 27.08.2006  |              |
      | 1            | 3       | BARN       | 08.11.2010  |              |

    Og dagens dato er 15.12.2024
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 3       |
      | 1            | 2       |
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                  | 22.11.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | UNDER_18_ÅR                                 |                  | 27.08.2006 | 26.08.2024 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP                            |                  | 27.08.2006 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 21.11.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR                                 |                  | 08.11.2010 | 07.11.2028 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP                            |                  | 08.11.2010 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 21.11.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                               |                  | 22.11.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 1            | 01.12.2023 | 31.07.2024 | ETTERBETALING_3MND | 0       | 10.11.2024       |                             |
      | 3       | 1            | 01.12.2023 | 31.07.2024 | ETTERBETALING_3MND | 0       | 10.11.2024       |                             |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 2       | 1            | 01.12.2023 | 31.07.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1310 |
      | 3       | 1            | 01.12.2023 | 31.07.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1310 |
      | 3       | 1            | 01.08.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 1            | 01.09.2024 | 31.10.2028 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                                | Ugyldige begrunnelser |
      | 01.12.2023 | 31.07.2024 | OPPHØR             |                                | ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TILBAKE_I_TID           |                       |
      | 01.08.2024 | 31.08.2024 | UTBETALING         |                                | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TREDJELANDSBORGER |                       |
      | 01.09.2024 | 31.10.2028 | UTBETALING         |                                |                                                                     |                       |
      | 01.11.2028 |            | OPPHØR             |                                |                                                                     |                       |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                                                | Eøsbegrunnelser | Fritekster |
      | 01.12.2023 | 31.07.2024 | ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TILBAKE_I_TID           |                 |            |
      | 01.08.2024 | 31.08.2024 | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TREDJELANDSBORGER |                 |            |
      | 01.09.2024 | 31.10.2028 |                                                                     |                 |            |
      | 01.11.2028 |            |                                                                     |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.08.2024 til 31.08.2024
      | Begrunnelse                                                         | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TREDJELANDSBORGER | STANDARD | Nei           | 08.11.10             | 1           | juli 2024                            |         | 1 510 | 10.11.24         |                         |                             |

  Scenario: Etter endret utbetaling begrunnelser skal bare inneholde personer som ble påvirket av endret utbetaling i forrige periode
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat        | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | FORTSATT_INNVILGET         | OMREGNING_18ÅR   | Ja                        | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | DELVIS_INNVILGET_OG_ENDRET | SØKNAD           | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 25.12.1977  |              |
      | 1            | 3       | BARN       | 15.01.2007  |              |
      | 1            | 5       | BARN       | 13.05.2009  |              |
      | 2            | 1       | SØKER      | 25.12.1977  |              |
      | 2            | 3       | BARN       | 15.01.2007  |              |
      | 2            | 5       | BARN       | 13.05.2009  |              |
      | 2            | 7       | BARN       | 28.07.2016  |              |
      | 2            | 8       | BARN       | 19.05.2020  |              |

    Og dagens dato er 28.01.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 2            | 8       |
      | 2            | 7       |
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                  | 01.11.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | GIFT_PARTNERSKAP                            |                  | 15.01.2007 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR                                 |                  | 15.01.2007 | 14.01.2025 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOSATT_I_RIKET,BOR_MED_SØKER,LOVLIG_OPPHOLD |                  | 01.11.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 5       | UNDER_18_ÅR                                 |                  | 13.05.2009 | 12.05.2027 | OPPFYLT  | Nei                  |                      |                  |
      | 5       | GIFT_PARTNERSKAP                            |                  | 13.05.2009 |            | OPPFYLT  | Nei                  |                      |                  |
      | 5       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 01.11.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                  | 01.11.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR                                 |                  | 15.01.2007 | 14.01.2025 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP                            |                  | 15.01.2007 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | LOVLIG_OPPHOLD,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 01.11.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 5       | UNDER_18_ÅR                                 |                  | 13.05.2009 | 12.05.2027 | OPPFYLT  | Nei                  |                      |                  |
      | 5       | GIFT_PARTNERSKAP                            |                  | 13.05.2009 |            | OPPFYLT  | Nei                  |                      |                  |
      | 5       | LOVLIG_OPPHOLD,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 01.11.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 7       | GIFT_PARTNERSKAP                            |                  | 28.07.2016 |            | OPPFYLT  | Nei                  |                      |                  |
      | 7       | UNDER_18_ÅR                                 |                  | 28.07.2016 | 27.07.2034 | OPPFYLT  | Nei                  |                      |                  |
      | 7       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 06.11.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 7       | BOR_MED_SØKER                               |                  | 30.07.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 8       | GIFT_PARTNERSKAP                            |                  | 19.05.2020 |            | OPPFYLT  | Nei                  |                      |                  |
      | 8       | UNDER_18_ÅR                                 |                  | 19.05.2020 | 18.05.2038 | OPPFYLT  | Nei                  |                      |                  |
      | 8       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 06.11.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 8       | BOR_MED_SØKER                               |                  | 30.07.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 7, 8    | 2            | 01.08.2024 | 31.08.2024 | ETTERBETALING_3MND | 0       | 12.12.2024       |                             |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3       | 1            | 01.12.2021 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 1            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 1            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 1            | 01.09.2024 | 31.12.2024 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 5       | 1            | 01.12.2021 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 5       | 1            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 5       | 1            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 5       | 1            | 01.09.2024 | 30.04.2027 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |

      | 3       | 2            | 01.12.2021 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 2            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 2            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 2            | 01.09.2024 | 31.12.2024 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 5       | 2            | 01.12.2021 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 5       | 2            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 5       | 2            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 5       | 2            | 01.09.2024 | 30.04.2027 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 7       | 2            | 01.08.2024 | 31.08.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1510 |
      | 7       | 2            | 01.09.2024 | 30.06.2034 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 8       | 2            | 01.08.2024 | 31.08.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 8       | 2            | 01.09.2024 | 30.04.2038 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                              | Ugyldige begrunnelser |
      | 01.09.2024 | 31.12.2024 | UTBETALING         |                                | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                              | Eøsbegrunnelser | Fritekster |
      | 01.09.2024 | 31.12.2024 | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.09.2024 til 31.12.2024
      | Begrunnelse                                       | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER | STANDARD |               | 28.07.16 og 19.05.20 | 2           | august 2024                          |         | 3 532 | 12.12.24         |                         |                             |


  Scenario: Etter endret utbetaling begrunnelser skal inneholde personer som hadde delt utbetaling ved endret utbetaling i forrige periode men ikke denne
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | ENDRET_UTBETALING   | SATSENDRING      | Ja                        | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | INNVILGET_OG_ENDRET | SØKNAD           | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 19.11.1983  |              |
      | 1            | 2       | BARN       | 11.03.2006  |              |
      | 1            | 3       | BARN       | 14.05.2008  |              |
      | 2            | 1       | SØKER      | 19.11.1983  |              |
      | 2            | 2       | BARN       | 11.03.2006  |              |
      | 2            | 3       | BARN       | 14.05.2008  |              |

    Og dagens dato er 02.02.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 2            | 3       |
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 01.04.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | UNDER_18_ÅR                   |                  | 11.03.2006 | 10.03.2024 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP              |                  | 11.03.2006 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.04.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED      | 01.04.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | GIFT_PARTNERSKAP              |                  | 14.05.2008 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR                   |                  | 14.05.2008 | 13.05.2026 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 01.04.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                 | DELT_BOSTED      | 01.04.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.04.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | GIFT_PARTNERSKAP              |                  | 11.03.2006 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                   |                  | 11.03.2006 | 10.03.2024 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.04.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED      | 01.04.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | GIFT_PARTNERSKAP              |                  | 14.05.2008 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR                   |                  | 14.05.2008 | 13.05.2026 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 01.04.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                 | DELT_BOSTED      | 01.04.2022 | 27.11.2023 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                 |                  | 28.11.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 3       | 2            | 01.12.2023 | 31.07.2024 | ETTERBETALING_3MND | 50      | 26.11.2024       |                             |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.05.2022 | 28.02.2023 | 527   | ORDINÆR_BARNETRYGD | 50      | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 542   | ORDINÆR_BARNETRYGD | 50      | 1083 |
      | 2       | 1            | 01.07.2023 | 31.12.2023 | 655   | ORDINÆR_BARNETRYGD | 50      | 1310 |
      | 2       | 1            | 01.01.2024 | 29.02.2024 | 755   | ORDINÆR_BARNETRYGD | 50      | 1510 |
      | 3       | 1            | 01.05.2022 | 28.02.2023 | 527   | ORDINÆR_BARNETRYGD | 50      | 1054 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 542   | ORDINÆR_BARNETRYGD | 50      | 1083 |
      | 3       | 1            | 01.07.2023 | 31.12.2023 | 655   | ORDINÆR_BARNETRYGD | 50      | 1310 |
      | 3       | 1            | 01.01.2024 | 31.08.2024 | 755   | ORDINÆR_BARNETRYGD | 50      | 1510 |
      | 3       | 1            | 01.09.2024 | 30.04.2026 | 883   | ORDINÆR_BARNETRYGD | 50      | 1766 |

      | 2       | 2            | 01.05.2022 | 28.02.2023 | 527   | ORDINÆR_BARNETRYGD | 50      | 1054 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 542   | ORDINÆR_BARNETRYGD | 50      | 1083 |
      | 2       | 2            | 01.07.2023 | 31.12.2023 | 655   | ORDINÆR_BARNETRYGD | 50      | 1310 |
      | 2       | 2            | 01.01.2024 | 29.02.2024 | 755   | ORDINÆR_BARNETRYGD | 50      | 1510 |
      | 3       | 2            | 01.05.2022 | 28.02.2023 | 527   | ORDINÆR_BARNETRYGD | 50      | 1054 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 542   | ORDINÆR_BARNETRYGD | 50      | 1083 |
      | 3       | 2            | 01.07.2023 | 31.12.2023 | 655   | ORDINÆR_BARNETRYGD | 50      | 1310 |
      | 3       | 2            | 01.01.2024 | 31.07.2024 | 755   | ORDINÆR_BARNETRYGD | 50      | 1510 |
      | 3       | 2            | 01.08.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 2            | 01.09.2024 | 30.04.2026 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Når vedtaksperiodene genereres for behandling 2


    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                              | Ugyldige begrunnelser |
      | 01.08.2024 | 31.08.2024 | UTBETALING         |                                | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                              | Eøsbegrunnelser | Fritekster |
      | 01.08.2024 | 31.08.2024 | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.08.2024 til 31.08.2024
      | Begrunnelse                                       | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER | STANDARD |               | 14.05.08             | 1           | juli 2024                            |         | 1 510 | 26.11.24         |                         |                             |