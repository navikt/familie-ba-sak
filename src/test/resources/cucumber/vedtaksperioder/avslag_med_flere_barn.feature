# language: no
# encoding: UTF-8

Egenskap: Avslagperioder med flere barn

  Scenario: Dersom det eksisterer en avslagsperiode med tom dato før barn er født, skal ikke barnet flettes inn i avslagsperioden
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | EØS                 | AVSLUTTET         |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 22.07.1988  |              |
      | 1            | 2       | BARN       | 21.01.2022  |              |
      | 1            | 3       | BARN       | 23.04.2025  |              |

    Og dagens dato er 13.08.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 3       |
      | 1            | 2       |
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser                                    | Vurderes etter   |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 20.05.2023 | 11.08.2024 | OPPFYLT      | Nei                  |                                                         | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD   |                              | 20.05.2023 |            | OPPFYLT      | Nei                  |                                                         | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   |                              | 12.08.2024 | 27.02.2025 | IKKE_OPPFYLT | Ja                   | AVSLAG_EØS_ARBEIDER_MER_ENN_25_PROSENT_I_ANNET_EØS_LAND | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 01.03.2025 |            | OPPFYLT      | Nei                  |                                                         | EØS_FORORDNINGEN |

      | 2       | GIFT_PARTNERSKAP |                              | 21.01.2022 |            | OPPFYLT      | Nei                  |                                                         |                  |
      | 2       | UNDER_18_ÅR      |                              | 21.01.2022 | 20.01.2040 | OPPFYLT      | Nei                  |                                                         |                  |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 20.05.2023 |            | OPPFYLT      | Nei                  |                                                         | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 20.05.2023 |            | OPPFYLT      | Nei                  |                                                         | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 20.05.2023 |            | OPPFYLT      | Nei                  |                                                         | EØS_FORORDNINGEN |

      | 3       | LOVLIG_OPPHOLD   |                              | 23.04.2025 |            | OPPFYLT      | Nei                  |                                                         | EØS_FORORDNINGEN |
      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 23.04.2025 |            | OPPFYLT      | Nei                  |                                                         | EØS_FORORDNINGEN |
      | 3       | GIFT_PARTNERSKAP |                              | 23.04.2025 |            | OPPFYLT      | Nei                  |                                                         |                  |
      | 3       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 23.04.2025 |            | OPPFYLT      | Nei                  |                                                         | EØS_FORORDNINGEN |
      | 3       | UNDER_18_ÅR      |                              | 23.04.2025 | 22.04.2043 | OPPFYLT      | Nei                  |                                                         |                  |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 3, 2    | 01.05.2025 |            | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | SE                             | SE                  |
      | 2       | 01.06.2023 | 31.08.2024 | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | SE                             | SE                  |
      | 2       | 01.04.2025 | 30.04.2025 | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | SE                             | SE                  |

    Og med utenlandsk periodebeløp
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 3, 2    | 05.2025   |           | 1            | 1325  | SEK         | MÅNEDLIG  | SE              |
      | 2       | 06.2023   | 08.2024   | 1            | 1250  | SEK         | MÅNEDLIG  | SE              |
      | 2       | 04.2025   | 04.2025   | 1            | 1250  | SEK         | MÅNEDLIG  | SE              |

    Og med valutakurser
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2       | 01.06.2023 | 30.06.2023 | 1            | 2023-05-31     | SEK         | 1.0323964980 | AUTOMATISK     |
      | 2       | 01.07.2023 | 31.07.2023 | 1            | 2023-06-30     | SEK         | 0.9914023125 | AUTOMATISK     |
      | 2       | 01.08.2023 | 31.08.2023 | 1            | 2023-07-31     | SEK         | 0.9647094353 | AUTOMATISK     |
      | 2       | 01.09.2023 | 30.09.2023 | 1            | 2023-08-31     | SEK         | 0.9777927890 | AUTOMATISK     |
      | 2       | 01.10.2023 | 31.10.2023 | 1            | 2023-09-29     | SEK         | 0.9758075005 | AUTOMATISK     |
      | 2       | 01.11.2023 | 30.11.2023 | 1            | 2023-10-31     | SEK         | 1.0038892412 | AUTOMATISK     |
      | 2       | 01.12.2023 | 31.12.2023 | 1            | 2023-11-30     | SEK         | 1.0253000665 | AUTOMATISK     |
      | 2       | 01.01.2024 | 31.01.2024 | 1            | 2023-12-29     | SEK         | 1.0130227109 | AUTOMATISK     |
      | 2       | 01.02.2024 | 29.02.2024 | 1            | 2024-01-31     | SEK         | 1.0073481124 | AUTOMATISK     |
      | 2       | 01.03.2024 | 31.03.2024 | 1            | 2024-02-29     | SEK         | 1.0246990638 | AUTOMATISK     |
      | 2       | 01.04.2024 | 30.04.2024 | 1            | 2024-03-27     | SEK         | 1.0153398227 | AUTOMATISK     |
      | 2       | 01.05.2024 | 31.05.2024 | 1            | 2024-04-30     | SEK         | 1.0052752489 | AUTOMATISK     |
      | 2       | 01.06.2024 | 30.06.2024 | 1            | 2024-05-31     | SEK         | 0.9966727957 | AUTOMATISK     |
      | 2       | 01.07.2024 | 31.07.2024 | 1            | 2024-06-28     | SEK         | 1.0032571856 | AUTOMATISK     |
      | 2       | 01.08.2024 | 31.08.2024 | 1            | 2024-07-31     | SEK         | 1.0176533907 | AUTOMATISK     |
      | 2       | 01.04.2025 | 30.04.2025 | 1            | 2025-03-31     | SEK         | 1.0519863582 | AUTOMATISK     |
      | 3, 2    | 01.05.2025 | 31.05.2025 | 1            | 2025-04-30     | SEK         | 1.0763341384 | AUTOMATISK     |
      | 3, 2    | 01.06.2025 | 30.06.2025 | 1            | 2025-05-30     | SEK         | 1.0613693843 | AUTOMATISK     |
      | 3, 2    | 01.07.2025 | 31.07.2025 | 1            | 2025-06-30     | SEK         | 1.0617234109 | AUTOMATISK     |
      | 3, 2    | 01.08.2025 |            | 1            | 2025-07-31     | SEK         | 1.0552543132 | AUTOMATISK     |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 2       | 1            | 01.06.2023 | 30.06.2023 | 433   | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 2       | 1            | 01.07.2023 | 31.07.2023 | 527   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.08.2023 | 31.08.2023 | 561   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.09.2023 | 30.09.2023 | 544   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.10.2023 | 31.10.2023 | 547   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.11.2023 | 30.11.2023 | 512   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.12.2023 | 31.12.2023 | 485   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.01.2024 | 31.01.2024 | 500   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.02.2024 | 29.02.2024 | 507   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.03.2024 | 31.03.2024 | 486   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.04.2024 | 30.04.2024 | 497   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.05.2024 | 31.05.2024 | 510   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.06.2024 | 30.06.2024 | 521   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.07.2024 | 31.07.2024 | 512   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.08.2024 | 31.08.2024 | 494   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.04.2025 | 30.04.2025 | 452   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.05.2025 | 31.05.2025 | 542   | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 2       | 1            | 01.06.2025 | 31.07.2025 | 562   | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 2       | 1            | 01.08.2025 | 31.12.2039 | 570   | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 1            | 01.05.2025 | 31.05.2025 | 542   | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 1            | 01.06.2025 | 31.07.2025 | 562   | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 1            | 01.08.2025 | 31.03.2043 | 570   | ORDINÆR_BARNETRYGD | 100     | 1968 |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                    | Ugyldige begrunnelser |
      | 01.09.2024 | 28.02.2025 | AVSLAG             | EØS_FORORDNINGEN               | AVSLAG_EØS_ARBEIDER_MER_ENN_25_PROSENT_I_ANNET_EØS_LAND |                       |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser | Eøsbegrunnelser                                         | Fritekster |
      | 01.09.2024 | 28.02.2025 |                      | AVSLAG_EØS_ARBEIDER_MER_ENN_25_PROSENT_I_ANNET_EØS_LAND |            |
      | 01.04.2043 |            |                      |                                                         |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.09.2024 til 28.02.2025
      | Begrunnelse                                             | Type | Barnas fødselsdatoer | Antall barn | Gjelder søker | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | AVSLAG_EØS_ARBEIDER_MER_ENN_25_PROSENT_I_ANNET_EØS_LAND | EØS  | 21.01.22             | 1           | Ja            |                  |                           |                       |                                |                     |