# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser for endret utbetaling med etterbetaling tre år tilbake i tid

  Bakgrunn:
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

  Scenario: Skal kunne begrunne utvidet for to barn etter endret utbetaling med etterbetaling tre år tilbake i tid
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
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 1       | 2            | 01.02.2020 | 31.05.2020 | ETTERBETALING_3ÅR | 0       | 30.06.2023       | 02.02.2020                  |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Gyldige Begrunnelser                                                            | Ugyldige Begrunnelser                                                                |
      | 01.02.2020 | 31.05.2020 | UTBETALING         |           | ENDRET_UTBETALING_ETTERBETALING_TRE_ÅR_TILBAKE_I_TID_KUN_UTVIDET_DEL_UTBETALING | ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TILBAKE_I_TID_KUN_UTVIDET_DEL_UTBETALING |
      | 01.06.2020 | 30.06.2022 | UTBETALING         |           | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_AAR_KUN_UTVIDET_DEL                   | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_KUN_UTVIDET_DEL                    |
      | 01.07.2022 | 31.01.2023 | UTBETALING         |           |                                                                                 |                                                                                      |
      | 01.02.2023 | 28.02.2023 | UTBETALING         |           |                                                                                 |                                                                                      |
      | 01.03.2023 |            | OPPHØR             |           |                                                                                 |                                                                                      |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                                                            | Eøsbegrunnelser | Fritekster |
      | 01.02.2020 | 31.05.2020 | ENDRET_UTBETALING_ETTERBETALING_TRE_ÅR_TILBAKE_I_TID_KUN_UTVIDET_DEL_UTBETALING |                 |            |
      | 01.06.2020 | 30.06.2022 | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_AAR_KUN_UTVIDET_DEL                   |                 |            |
      | 01.07.2022 | 31.01.2023 |                                                                                 |                 |            |
      | 01.02.2023 | 28.02.2023 |                                                                                 |                 |            |
      | 01.03.2023 |            |                                                                                 |                 |            |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.02.2020 til 31.05.2020
      | Begrunnelse                                                                     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet     |
      | ENDRET_UTBETALING_ETTERBETALING_TRE_ÅR_TILBAKE_I_TID_KUN_UTVIDET_DEL_UTBETALING | Ja            |                      | 0           | januar 2020                          | NB      | 1 581 | 30.06.23         | SØKER_HAR_RETT_MEN_FÅR_IKKE |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.06.2020 til 30.06.2022
      | Begrunnelse                                                   | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_AAR_KUN_UTVIDET_DEL | Ja            | 13.02.05 og 06.11.10 | 2           | mai 2020                             | NB      | 2 635 | 30.06.23         | SØKER_FÅR_UTVIDET       |
