# language: no
# encoding: UTF-8

Egenskap: Vilkårsvurderingssteg

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus | Behandlingssteg      | Underkategori | Behandlingstype |
      | 1            | 1        |                     | ENDRET_UTBETALING   | SATSENDRING      | Ja                        | EØS                 | AVSLUTTET         | BEHANDLING_AVSLUTTET | UTVIDET       | REVURDERING     |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | TEKNISK_ENDRING  | Nei                       | EØS                 | UTREDES           | VILKÅRSVURDERING     | UTVIDET       | TEKNISK_ENDRING |
      | 3            | 1        | 1                   | ENDRET_UTBETALING   | NYE_OPPLYSNINGER | Nei                       | EØS                 | UTREDES           | VILKÅRSVURDERING     | UTVIDET       | REVURDERING     |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1, 2, 3      | 1       | SØKER      | 20.03.1995  |              |
      | 1, 2, 3      | 2       | BARN       | 15.07.2018  |              |

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår             | Utdypende vilkår                    | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD     |                                     | 01.03.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET     | OMFATTET_AV_NORSK_LOVGIVNING_UTLAND | 01.03.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | UTVIDET_BARNETRYGD |                                     | 14.11.2021 |            | OPPFYLT  | Nei                  |                      |                  |

      | 2       | UNDER_18_ÅR        |                                     | 15.07.2018 | 14.07.2036 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP   |                                     | 15.07.2018 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET     | BARN_BOR_I_EØS                      | 14.11.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER      | BARN_BOR_I_EØS_MED_SØKER            | 14.11.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD     |                                     | 14.11.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet  | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.12.2021 |          | NORGE_ER_SEKUNDÆRLAND | 1            | MOTTAR_UFØRETRYGD | I_ARBEID                  | NO                    | SE                             | SE                  |

    Og med utenlandsk periodebeløp
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2       | 12.2021   |           | 1            | 1250  | SEK         | MÅNEDLIG  | SE              |

    Og med valutakurser
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2       | 01.12.2021 | 31.12.2021 | 1            | 2021-12-31     | SEK         | 0.9744885516 | MANUELL        |
      | 2       | 01.01.2022 | 31.12.2022 | 1            | 2022-12-30     | SEK         | 0.9453325900 | MANUELL        |
      | 2       | 01.01.2023 |            | 1            | 2023-06-07     | SEK         | 1.0147950626 | MANUELL        |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.12.2021 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 1            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 1       | 1            | 01.07.2023 | 30.06.2036 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 1            | 01.12.2021 | 31.12.2021 | 436   | ORDINÆR_BARNETRYGD | 100     | 1654 |
      | 2       | 1            | 01.01.2022 | 31.12.2022 | 495   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 1            | 01.01.2023 | 28.02.2023 | 408   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 455   | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 2       | 1            | 01.07.2023 | 30.06.2024 | 498   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.07.2024 | 30.06.2036 | 42    | ORDINÆR_BARNETRYGD | 100     | 1310 |

  Scenario: skal generere valutakurser for behandling med type teknisk endring men bare for perioden etter praksisendringen

    Og dagens dato er 01.08.2024
    Og lag personresultater for behandling 2
    Og kopier vilkårresultater fra behandling 1 til behandling 2
    Og kopier kompetanser fra behandling 1 til behandling 2
    Og kopier utenlandsk periodebeløp fra behandling 1 til behandling 2

    Når vi utfører vilkårsvurderingssteget for behandling 2

    Så forvent følgende valutakurser for behandling 2
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2       | 01.12.2021 | 31.12.2021 | 2            | 2021-12-31     | SEK         | 0.9744885516 | MANUELL        |
      | 2       | 01.01.2022 | 31.12.2022 | 2            | 2022-12-30     | SEK         | 0.9453325900 | MANUELL        |
      | 2       | 01.01.2023 | 31.05.2024 | 2            | 2023-06-07     | SEK         | 1.0147950626 | MANUELL        |
      | 2       | 01.06.2024 | 30.06.2024 | 2            | 2024-05-31     | SEK         | 10           | AUTOMATISK     |
      | 2       | 01.07.2024 | 31.07.2024 | 2            | 2024-06-28     | SEK         | 10           | AUTOMATISK     |
      | 2       | 01.08.2024 |            | 2            | 2024-07-31     | SEK         | 10           | AUTOMATISK     |

  Scenario: skal generere valutakurser for behandling med type revurdering men bare for perioden etter praksisendringen

    Og dagens dato er 01.08.2024
    Og lag personresultater for behandling 3
    Og kopier vilkårresultater fra behandling 1 til behandling 3
    Og kopier kompetanser fra behandling 1 til behandling 3
    Og kopier utenlandsk periodebeløp fra behandling 1 til behandling 3

    Når vi utfører vilkårsvurderingssteget for behandling 3

    Så forvent følgende valutakurser for behandling 3
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2       | 01.12.2021 | 31.12.2021 | 3            | 2021-12-31     | SEK         | 0.9744885516 | MANUELL        |
      | 2       | 01.01.2022 | 31.12.2022 | 3            | 2022-12-30     | SEK         | 0.9453325900 | MANUELL        |
      | 2       | 01.01.2023 | 31.05.2024 | 3            | 2023-06-07     | SEK         | 1.0147950626 | MANUELL        |
      | 2       | 01.06.2024 | 30.06.2024 | 3            | 2024-05-31     | SEK         | 10           | AUTOMATISK     |
      | 2       | 01.07.2024 | 31.07.2024 | 3            | 2024-06-28     | SEK         | 10           | AUTOMATISK     |
      | 2       | 01.08.2024 |            | 3            | 2024-07-31     | SEK         | 10           | AUTOMATISK     |
