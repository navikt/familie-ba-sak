# language: no
# encoding: UTF-8

Egenskap: Delt bosted og endret utbetaling, ikke alle barn har utbetaling

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | ENDRET_UTBETALING   | SATSENDRING      | Ja                        | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | INNVILGET_OG_ENDRET | SØKNAD           | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 11.05.1987  |              |
      | 1            | 3       | BARN       | 26.11.2014  |              |
      | 2            | 1       | SØKER      | 11.05.1987  |              |
      | 2            | 2       | BARN       | 16.05.2006  |              |
      | 2            | 3       | BARN       | 26.11.2014  |              |

  Scenario: Antall barn - Ett barn skal utbetales delt bosted, men ikke det andre
    Og følgende dagens dato 05.06.2024
    Og lag personresultater for begrunnelse for behandling 1
    Og lag personresultater for begrunnelse for behandling 2

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | GIFT_PARTNERSKAP                            |                  | 26.11.2014 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR                                 |                  | 26.11.2014 | 25.11.2032 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER |                  | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og legg til nye vilkårresultater for begrunnelse for behandling 2
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

    Og med endrede utbetalinger for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak       | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 3       | 2            | 01.06.2023 | 30.04.2024 | DELT_BOSTED | 100     | 10.04.2024       | 2023-05-01                  |
      | 2       | 2            | 01.06.2023 | 29.02.2024 | DELT_BOSTED | 0       | 18.02.2024       | 2023-05-01                  |

    Og med andeler tilkjent ytelse for begrunnelse
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
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_MOTTATT_FULL_ORDINÆR_ETTERBETALT_UTVIDET_NY | STANDARD |               | 26.11.14             | 1           | mai 2023                             |         | 1 083 | 18.02.24         | SØKER_FÅR_UTVIDET       |                             |
