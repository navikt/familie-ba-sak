# language: no
# encoding: UTF-8

Egenskap: EØS-Primærlandsbegrunnelser for innvilget eller økning skal dukke opp selv om kompetanse er uendret

  Bakgrunn:
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

  Scenario: Endret utbetaling fører til at start av primærlandsperiode ikke skal utbetales. Primærlandsperioden som faktisk skal utbetales må allikevel kunne begrunnes med primærlandsbegrunnelser.
    Og dagens dato er 20.05.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 2       |
      | 1            | 1       |
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår             | Utdypende vilkår                            | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser                           | Vurderes etter   |
      | 1       | BOSATT_I_RIKET     | ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING | 01.06.2022 | 29.12.2022 | OPPFYLT      | Nei                  |                                                | EØS_FORORDNINGEN |
      | 1       | UTVIDET_BARNETRYGD |                                             | 01.06.2022 |            | OPPFYLT      | Nei                  |                                                |                  |
      | 1       | LOVLIG_OPPHOLD     |                                             | 01.06.2022 |            | OPPFYLT      | Nei                  |                                                | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET     | ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING | 30.12.2022 | 31.01.2023 | OPPFYLT      | Nei                  |                                                | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET     |                                             | 01.02.2023 | 01.05.2023 | IKKE_OPPFYLT | Ja                   | AVSLAG_SEKUNDÆRLAND_INGEN_AV_FORELDRENE_JOBBER | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET     | ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING | 02.05.2023 |            | OPPFYLT      | Nei                  |                                                | EØS_FORORDNINGEN |

      | 2       | UNDER_18_ÅR        |                                             | 14.02.2021 | 13.02.2039 | OPPFYLT      | Nei                  |                                                |                  |
      | 2       | GIFT_PARTNERSKAP   |                                             | 14.02.2021 |            | OPPFYLT      | Nei                  |                                                |                  |
      | 2       | LOVLIG_OPPHOLD     |                                             | 01.06.2022 |            | OPPFYLT      | Nei                  |                                                | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER      | BARN_BOR_I_EØS_MED_SØKER                    | 01.06.2022 |            | OPPFYLT      | Nei                  |                                                | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET     | BARN_BOR_I_EØS                              | 01.06.2022 |            | OPPFYLT      | Nei                  |                                                | EØS_FORORDNINGEN |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.06.2023 |            | NORGE_ER_PRIMÆRLAND   | 1            | INAKTIV          | ARBEIDER                  | DK                    | NO                             | DK                  |
      | 2       | 01.07.2022 | 31.01.2023 | NORGE_ER_SEKUNDÆRLAND | 1            | I_ARBEID         | ARBEIDER                  | DK                    | NO                             | DK                  |

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
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                           | Ugyldige begrunnelser |
      | 01.04.2024 | 30.04.2025 | UTBETALING         | EØS_FORORDNINGEN               | INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_STANDARD |                       |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser | Eøsbegrunnelser                                | Fritekster |
      | 01.04.2024 | 30.04.2025 |                      | INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_STANDARD |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.04.2024 til 30.04.2025
      | Begrunnelse                                    | Type | Barnas fødselsdatoer | Antall barn | Målform | Annen forelders aktivitetsland | Barnets bostedsland | Søkers aktivitetsland | Annen forelders aktivitet | Søkers aktivitet | Gjelder søker |
      | INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_STANDARD | EØS  | 14.02.21             | 1           | NB      | Norge                          | Danmark             | Danmark               | ARBEIDER                  | INAKTIV          | Nei           |