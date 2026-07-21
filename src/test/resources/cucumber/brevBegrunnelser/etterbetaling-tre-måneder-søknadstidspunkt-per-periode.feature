# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser for etterbetaling tre måneder skal hente riktig søknadstidspunkt per periode

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status    |
      | 1        | NORMAL     | OPPRETTET |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | EØS                 | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 23.11.1986  |              |
      | 1            | 2       | BARN       | 25.11.2009  |              |
      | 1            | 3       | BARN       | 05.10.2011  |              |
      | 1            | 4       | BARN       | 16.11.2018  |              |

  Scenario: Skal hente riktig søknadstidspunkt for hver periode når barna har ulike søknadstidspunkt
    Og dagens dato er 21.07.2026
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 4       |
      | 1            | 3       |
      | 1            | 2       |
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår                            | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                                             | 29.10.2013 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING | 29.10.2013 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | UNDER_18_ÅR      |                                             | 25.11.2009 | 24.11.2027 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP |                                             | 25.11.2009 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | LOVLIG_OPPHOLD   |                                             | 29.10.2013 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS                              | 29.10.2013 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER                    | 29.10.2013 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 3       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER                    | 05.10.2011 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | GIFT_PARTNERSKAP |                                             | 05.10.2011 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR      |                                             | 05.10.2011 | 04.10.2029 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_EØS                              | 05.10.2011 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | LOVLIG_OPPHOLD   |                                             | 05.10.2011 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 4       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER                    | 16.11.2018 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 4       | GIFT_PARTNERSKAP |                                             | 16.11.2018 |            | OPPFYLT  | Nei                  |                      |                  |
      | 4       | BOSATT_I_RIKET   | BARN_BOR_I_EØS                              | 16.11.2018 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 4       | UNDER_18_ÅR      |                                             | 16.11.2018 | 15.11.2036 | OPPFYLT  | Nei                  |                      |                  |
      | 4       | LOVLIG_OPPHOLD   |                                             | 16.11.2018 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.10.2025 | 31.12.2025 | NORGE_ER_SEKUNDÆRLAND | 1            | I_ARBEID         | ARBEIDER                  | SE                    | NO                             | SE                  |
      | 3, 2, 4 | 01.01.2026 |            | NORGE_ER_SEKUNDÆRLAND | 1            | I_ARBEID         | ARBEIDER                  | SE                    | NO                             | SE                  |

    Og med utenlandsk periodebeløp
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2       | 10.2025   | 12.2025   | 1            | 1493  | SEK         | MÅNEDLIG  | SE              |
      | 3, 2, 4 | 01.2026   | 06.2026   | 1            | 1493  | SEK         | MÅNEDLIG  | SE              |
      | 3, 4    | 07.2026   |           | 1            | 1325  | SEK         | MÅNEDLIG  | SE              |
      | 2       | 07.2026   |           | 1            | 0     | SEK         | MÅNEDLIG  | SE              |

    Og med valutakurser
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2       | 01.10.2025 | 31.10.2025 | 1            | 2025-09-30     | SEK         | 1.0605978384 | AUTOMATISK     |
      | 2       | 01.11.2025 | 30.11.2025 | 1            | 2025-10-31     | SEK         | 1.0662242563 | AUTOMATISK     |
      | 2       | 01.12.2025 | 31.12.2025 | 1            | 2025-11-28     | SEK         | 1.0724736770 | AUTOMATISK     |
      | 3, 2, 4 | 01.01.2026 | 31.01.2026 | 1            | 2025-12-31     | SEK         | 1.0943954165 | AUTOMATISK     |
      | 3, 2, 4 | 01.02.2026 | 28.02.2026 | 1            | 2026-01-30     | SEK         | 1.0825055843 | AUTOMATISK     |
      | 3, 2, 4 | 01.03.2026 | 31.03.2026 | 1            | 2026-02-27     | SEK         | 1.0510300723 | AUTOMATISK     |
      | 3, 2, 4 | 01.04.2026 | 30.04.2026 | 1            | 2026-03-31     | SEK         | 1.0246276158 | AUTOMATISK     |
      | 3, 2, 4 | 01.05.2026 | 31.05.2026 | 1            | 2026-04-30     | SEK         | 1.0052323707 | AUTOMATISK     |
      | 3, 2, 4 | 01.06.2026 | 30.06.2026 | 1            | 2026-05-29     | SEK         | 1.0001392499 | AUTOMATISK     |
      | 3, 2, 4 | 01.07.2026 |            | 1            | 2026-06-30     | SEK         | 1.0195610042 | AUTOMATISK     |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 1            | 01.11.2013 | 30.09.2025 | ETTERBETALING_3MND | 0       | 28.01.2026       |                             |
      | 3       | 1            | 01.11.2013 | 31.12.2025 | ETTERBETALING_3MND | 0       | 28.04.2026       |                             |
      | 4       | 1            | 01.12.2018 | 31.12.2025 | ETTERBETALING_3MND | 0       | 28.04.2026       |                             |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 2       | 1            | 01.11.2013 | 28.02.2019 | 0     | ORDINÆR_BARNETRYGD | 0       | 970  |
      | 2       | 1            | 01.03.2019 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1083 |
      | 2       | 1            | 01.07.2023 | 31.12.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1310 |
      | 2       | 1            | 01.01.2024 | 31.08.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1510 |
      | 2       | 1            | 01.09.2024 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 1            | 01.05.2025 | 30.09.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 1            | 01.10.2025 | 31.10.2025 | 385   | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 2       | 1            | 01.11.2025 | 30.11.2025 | 377   | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 2       | 1            | 01.12.2025 | 31.12.2025 | 367   | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 2       | 1            | 01.01.2026 | 31.01.2026 | 335   | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 2       | 1            | 01.02.2026 | 28.02.2026 | 396   | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 2       | 1            | 01.03.2026 | 31.03.2026 | 443   | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 2       | 1            | 01.04.2026 | 30.04.2026 | 483   | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 2       | 1            | 01.05.2026 | 31.05.2026 | 512   | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 2       | 1            | 01.06.2026 | 30.06.2026 | 519   | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 2       | 1            | 01.07.2026 | 31.10.2027 | 2012  | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 3       | 1            | 01.11.2013 | 28.02.2019 | 0     | ORDINÆR_BARNETRYGD | 0       | 970  |
      | 3       | 1            | 01.03.2019 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1083 |
      | 3       | 1            | 01.07.2023 | 31.12.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1310 |
      | 3       | 1            | 01.01.2024 | 31.08.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1510 |
      | 3       | 1            | 01.09.2024 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 3       | 1            | 01.05.2025 | 31.12.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 3       | 1            | 01.01.2026 | 31.01.2026 | 335   | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 1            | 01.02.2026 | 28.02.2026 | 396   | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 3       | 1            | 01.03.2026 | 31.03.2026 | 443   | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 3       | 1            | 01.04.2026 | 30.04.2026 | 483   | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 3       | 1            | 01.05.2026 | 31.05.2026 | 512   | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 3       | 1            | 01.06.2026 | 30.06.2026 | 519   | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 3       | 1            | 01.07.2026 | 30.09.2029 | 662   | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 4       | 1            | 01.12.2018 | 28.02.2019 | 0     | ORDINÆR_BARNETRYGD | 0       | 970  |
      | 4       | 1            | 01.03.2019 | 31.08.2020 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 4       | 1            | 01.09.2020 | 31.08.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1354 |
      | 4       | 1            | 01.09.2021 | 31.12.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1654 |
      | 4       | 1            | 01.01.2022 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 4       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 4       | 1            | 01.07.2023 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 4       | 1            | 01.05.2025 | 31.12.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 4       | 1            | 01.01.2026 | 31.01.2026 | 335   | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 4       | 1            | 01.02.2026 | 28.02.2026 | 396   | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 4       | 1            | 01.03.2026 | 31.03.2026 | 443   | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 4       | 1            | 01.04.2026 | 30.04.2026 | 483   | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 4       | 1            | 01.05.2026 | 31.05.2026 | 512   | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 4       | 1            | 01.06.2026 | 30.06.2026 | 519   | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 4       | 1            | 01.07.2026 | 31.10.2036 | 662   | ORDINÆR_BARNETRYGD | 100     | 2012 |

    Når vedtaksperiodene genereres for behandling 1

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                                          | Eøsbegrunnelser | Fritekster |
      | 01.11.2013 | 30.11.2018 | ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TILBAKE_I_TID_SED |                 |            |
      | 01.12.2018 | 30.09.2025 | ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TILBAKE_I_TID     |                 |            |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 1 i periode 01.11.2013 til 30.11.2018
      | Begrunnelse                                                   | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TILBAKE_I_TID_SED | STANDARD |               | 25.11.09 og 05.10.11 | 2           | oktober 2013                         | NB      | 0     | 28.01.26         |                         |                             |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 1 i periode 01.12.2018 til 30.09.2025
      | Begrunnelse                                               | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TILBAKE_I_TID | STANDARD |               | 16.11.18             | 1           | november 2018                        | NB      | 0     | 28.04.26         |                         |                             |
