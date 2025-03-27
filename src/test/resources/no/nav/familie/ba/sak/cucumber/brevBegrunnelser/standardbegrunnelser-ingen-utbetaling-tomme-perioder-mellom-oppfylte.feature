# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder med OPPHØR/INGEN_UTBETALING skal inneholde begrunnelser selv om det ikke finnes vilkår-resultater i samme periode

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status    |
      | 1        | NORMAL     | OPPRETTET |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat  | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | INNVILGET_OG_OPPHØRT | SØKNAD           | Nei                       | EØS                 | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 22.07.1983  |              |
      | 1            | 2       | BARN       | 12.07.2014  |              |
      | 1            | 3       | BARN       | 05.02.2021  |              |

  Scenario: Dersom det ikke er lagt inn vilkår-resultater som er IKKE_OPPFYLT, men man har latt være å fylle inn noe i perioder som ikke er oppfylt skal man fortsatt kunne begrunne resulterende vedtaksperioder med OPPHØR/INGEN_UTBETALING
    Og dagens dato er 26.03.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 3       |
      | 1            | 2       |
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår                            | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                                             | 31.01.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING | 31.01.2022 | 17.06.2022 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING | 22.09.2022 | 31.01.2023 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING | 11.05.2023 | 31.07.2023 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING | 30.08.2023 | 30.11.2023 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING | 20.02.2024 | 31.03.2024 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING | 13.08.2024 | 30.09.2024 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING | 19.11.2024 | 20.12.2024 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | GIFT_PARTNERSKAP |                                             | 12.07.2014 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR      |                                             | 12.07.2014 | 11.07.2032 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS                              | 31.01.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                                             | 31.01.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER                    | 31.01.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_EØS                              | 05.02.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | GIFT_PARTNERSKAP |                                             | 05.02.2021 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR      |                                             | 05.02.2021 | 04.02.2039 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER                    | 05.02.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | LOVLIG_OPPHOLD   |                                             | 05.02.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2, 3    | 01.02.2022 | 30.06.2022 | NORGE_ER_SEKUNDÆRLAND | 1            | I_ARBEID         | ARBEIDER                  | LT                    | NO                             | LT                  |
      | 2, 3    | 01.10.2022 | 31.01.2023 | NORGE_ER_SEKUNDÆRLAND | 1            | I_ARBEID         | ARBEIDER                  | LT                    | NO                             | LT                  |
      | 2, 3    | 01.06.2023 | 31.07.2023 | NORGE_ER_SEKUNDÆRLAND | 1            | I_ARBEID         | ARBEIDER                  | LT                    | NO                             | LT                  |
      | 2, 3    | 01.09.2023 | 30.11.2023 | NORGE_ER_SEKUNDÆRLAND | 1            | I_ARBEID         | ARBEIDER                  | LT                    | NO                             | LT                  |
      | 2, 3    | 01.03.2024 | 31.03.2024 | NORGE_ER_SEKUNDÆRLAND | 1            | I_ARBEID         | ARBEIDER                  | LT                    | NO                             | LT                  |
      | 2, 3    | 01.09.2024 | 30.09.2024 | NORGE_ER_SEKUNDÆRLAND | 1            | I_ARBEID         | ARBEIDER                  | LT                    | NO                             | LT                  |
      | 2, 3    | 01.12.2024 | 31.12.2024 | NORGE_ER_SEKUNDÆRLAND | 1            | I_ARBEID         | ARBEIDER                  | LT                    | NO                             | LT                  |

    Og med utenlandsk periodebeløp
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2, 3    | 02.2022   | 05.2022   | 1            | 73.5  | EUR         | MÅNEDLIG  | LT              |
      | 2, 3    | 03.2024   | 03.2024   | 1            | 96.25 | EUR         | MÅNEDLIG  | LT              |
      | 2, 3    | 06.2022   | 06.2022   | 1            | 80.5  | EUR         | MÅNEDLIG  | LT              |
      | 2, 3    | 10.2022   | 12.2022   | 1            | 80.5  | EUR         | MÅNEDLIG  | LT              |
      | 2, 3    | 01.2023   | 01.2023   | 1            | 85.75 | EUR         | MÅNEDLIG  | LT              |
      | 2, 3    | 06.2023   | 07.2023   | 1            | 85.75 | EUR         | MÅNEDLIG  | LT              |
      | 2, 3    | 09.2024   | 09.2024   | 1            | 96.25 | EUR         | MÅNEDLIG  | LT              |
      | 2, 3    | 12.2024   | 12.2024   | 1            | 96.25 | EUR         | MÅNEDLIG  | LT              |
      | 2, 3    | 09.2023   | 11.2023   | 1            | 85.75 | EUR         | MÅNEDLIG  | LT              |

    Og med valutakurser
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs    | Vurderingsform |
      | 2, 3    | 01.03.2024 | 31.03.2024 | 1            | 2024-02-29     | EUR         | 11.492  | AUTOMATISK     |
      | 2, 3    | 01.02.2022 | 28.02.2022 | 1            | 2022-01-31     | EUR         | 10.0085 | AUTOMATISK     |
      | 2, 3    | 01.03.2022 | 31.03.2022 | 1            | 2022-02-28     | EUR         | 9.9465  | AUTOMATISK     |
      | 2, 3    | 01.04.2022 | 30.04.2022 | 1            | 2022-03-31     | EUR         | 9.711   | AUTOMATISK     |
      | 2, 3    | 01.05.2022 | 31.05.2022 | 1            | 2022-04-29     | EUR         | 9.7525  | AUTOMATISK     |
      | 2, 3    | 01.06.2022 | 30.06.2022 | 1            | 2022-05-31     | EUR         | 10.0983 | AUTOMATISK     |
      | 2, 3    | 01.10.2022 | 31.10.2022 | 1            | 2022-09-30     | EUR         | 10.5838 | AUTOMATISK     |
      | 2, 3    | 01.11.2022 | 30.11.2022 | 1            | 2022-10-31     | EUR         | 10.3028 | AUTOMATISK     |
      | 2, 3    | 01.12.2022 | 31.12.2022 | 1            | 2022-11-30     | EUR         | 10.2648 | AUTOMATISK     |
      | 2, 3    | 01.01.2023 | 31.01.2023 | 1            | 2022-12-30     | EUR         | 10.5138 | AUTOMATISK     |
      | 2, 3    | 01.06.2023 | 30.06.2023 | 1            | 2023-05-31     | EUR         | 12.0045 | AUTOMATISK     |
      | 2, 3    | 01.07.2023 | 31.07.2023 | 1            | 2023-06-30     | EUR         | 11.704  | AUTOMATISK     |
      | 2, 3    | 01.10.2023 | 31.10.2023 | 1            | 2023-09-29     | EUR         | 11.2535 | AUTOMATISK     |
      | 2, 3    | 01.11.2023 | 30.11.2023 | 1            | 2023-10-31     | EUR         | 11.8735 | AUTOMATISK     |
      | 2, 3    | 01.09.2024 | 30.09.2024 | 1            | 2024-08-30     | EUR         | 11.662  | AUTOMATISK     |
      | 2, 3    | 01.12.2024 | 31.12.2024 | 1            | 2024-11-29     | EUR         | 11.6805 | AUTOMATISK     |
      | 2, 3    | 01.09.2023 | 30.09.2023 | 1            | 2023-08-31     | EUR         | 11.58   | AUTOMATISK     |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 2       | 1            | 01.02.2022 | 28.02.2022 | 319   | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.03.2022 | 31.03.2022 | 323   | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.04.2022 | 30.04.2022 | 341   | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.05.2022 | 31.05.2022 | 338   | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.06.2022 | 30.06.2022 | 242   | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.10.2022 | 31.10.2022 | 203   | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.11.2022 | 30.11.2022 | 225   | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.12.2022 | 31.12.2022 | 228   | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.01.2023 | 31.01.2023 | 153   | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.06.2023 | 30.06.2023 | 54    | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 31.07.2023 | 307   | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 1            | 01.09.2023 | 30.09.2023 | 318   | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 1            | 01.10.2023 | 31.10.2023 | 346   | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 1            | 01.11.2023 | 30.11.2023 | 292   | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 1            | 01.03.2024 | 31.03.2024 | 404   | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 1            | 01.09.2024 | 30.09.2024 | 644   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.12.2024 | 31.12.2024 | 642   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.02.2022 | 28.02.2022 | 941   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 1            | 01.03.2022 | 31.03.2022 | 945   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 1            | 01.04.2022 | 30.04.2022 | 963   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 1            | 01.05.2022 | 31.05.2022 | 960   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 1            | 01.06.2022 | 30.06.2022 | 864   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 1            | 01.10.2022 | 31.10.2022 | 825   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 1            | 01.11.2022 | 30.11.2022 | 847   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 1            | 01.12.2022 | 31.12.2022 | 850   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 1            | 01.01.2023 | 31.01.2023 | 775   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 1            | 01.06.2023 | 30.06.2023 | 694   | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 3       | 1            | 01.07.2023 | 31.07.2023 | 763   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.09.2023 | 30.09.2023 | 774   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.10.2023 | 31.10.2023 | 802   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.11.2023 | 30.11.2023 | 748   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.03.2024 | 31.03.2024 | 660   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.09.2024 | 30.09.2024 | 644   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.12.2024 | 31.12.2024 | 642   | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Når vedtaksperiodene genereres for behandling 1


    # Tilfeldig valgt "Gyldige begrunnelser". Tester her kun at en av standardbegrunnelsene for opphør EØS dukker opp.
    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser           | Ugyldige begrunnelser |
      | 01.07.2022 | 30.09.2022 | OPPHØR             | EØS_FORORDNINGEN               | OPPHOR_UGYLDIG_KONTONUMMER_EØS |                       |
      | 01.02.2023 | 31.05.2023 | OPPHØR             | EØS_FORORDNINGEN               | OPPHOR_UGYLDIG_KONTONUMMER_EØS |                       |
      | 01.08.2023 | 31.08.2023 | OPPHØR             | EØS_FORORDNINGEN               | OPPHOR_UGYLDIG_KONTONUMMER_EØS |                       |
      | 01.12.2023 | 29.02.2024 | OPPHØR             | EØS_FORORDNINGEN               | OPPHOR_UGYLDIG_KONTONUMMER_EØS |                       |
      | 01.04.2024 | 31.08.2024 | OPPHØR             | EØS_FORORDNINGEN               | OPPHOR_UGYLDIG_KONTONUMMER_EØS |                       |
      | 01.10.2024 | 30.11.2024 | OPPHØR             | EØS_FORORDNINGEN               | OPPHOR_UGYLDIG_KONTONUMMER_EØS |                       |
      | 01.01.2025 |            | OPPHØR             | EØS_FORORDNINGEN               | OPPHOR_UGYLDIG_KONTONUMMER_EØS |                       |
