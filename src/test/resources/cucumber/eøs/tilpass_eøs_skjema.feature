# language: no
# encoding: UTF-8

Egenskap: Tilpassing av EØS-skjemaer

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak         | Behandlingstype | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus | Behandlingssteg      |
      | 1            | 1        |                     | ENDRET_UTBETALING   | MÅNEDLIG_VALUTAJUSTERING | REVURDERING     | Ja                        | EØS                 | AVSLUTTET         | BEHANDLING_AVSLUTTET |
      | 2            | 1        | 1                   | IKKE_VURDERT        | NYE_OPPLYSNINGER         | REVURDERING     | Nei                       | EØS                 | UTREDES           | VILKÅRSVURDERING     |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 27.05.1983  |              |
      | 1            | 4       | BARN       | 02.07.2015  |              |
      | 2            | 1       | SØKER      | 27.05.1983  |              |
      | 2            | 4       | BARN       | 02.07.2015  |              |

  Scenario: Skal fjerne perioder som er fremover i tid
    Og dagens dato er 01.12.2024
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår             | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD     |                              | 01.08.2018 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET     | OMFATTET_AV_NORSK_LOVGIVNING | 01.08.2018 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 1       | UTVIDET_BARNETRYGD |                              | 26.07.2023 |            | OPPFYLT  | Nei                  |                  |

      | 4       | GIFT_PARTNERSKAP   |                              | 02.07.2015 |            | OPPFYLT  | Nei                  |                  |
      | 4       | UNDER_18_ÅR        |                              | 02.07.2015 | 01.07.2033 | OPPFYLT  | Nei                  |                  |
      | 4       | LOVLIG_OPPHOLD     |                              | 01.12.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 4       | BOSATT_I_RIKET     | BARN_BOR_I_NORGE             | 01.12.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 4       | BOR_MED_SØKER      | BARN_BOR_I_NORGE_MED_SØKER   | 01.12.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår             | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD     |                              | 01.08.2018 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET     | OMFATTET_AV_NORSK_LOVGIVNING | 01.08.2018 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 1       | UTVIDET_BARNETRYGD |                              | 26.07.2023 |            | OPPFYLT  | Nei                  |                  |

      | 4       | GIFT_PARTNERSKAP   |                              | 02.07.2015 |            | OPPFYLT  | Nei                  |                  |
      | 4       | UNDER_18_ÅR        |                              | 02.07.2015 | 01.07.2033 | OPPFYLT  | Nei                  |                  |
      | 4       | BOR_MED_SØKER      | BARN_BOR_I_NORGE_MED_SØKER   | 01.12.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 4       | BOSATT_I_RIKET     | BARN_BOR_I_NORGE             | 01.12.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 4       | LOVLIG_OPPHOLD     |                              | 01.12.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 4       | 01.01.2023 | 30.09.2027 | NORGE_ER_SEKUNDÆRLAND | 1            | INAKTIV          | I_ARBEID                  | NO                    | SE                             | NO                  |
      | 4       | 01.10.2027 | 31.10.2030 | NORGE_ER_SEKUNDÆRLAND | 1            | INAKTIV          | I_ARBEID                  | NO                    | SE                             | NO                  |
      | 4       | 01.11.2030 | 30.06.2033 | NORGE_ER_SEKUNDÆRLAND | 1            | INAKTIV          | I_ARBEID                  | NO                    | SE                             | NO                  |

      | 4       | 01.01.2023 | 30.09.2027 | NORGE_ER_SEKUNDÆRLAND | 2            | INAKTIV          | I_ARBEID                  | NO                    | SE                             | NO                  |
      | 4       | 01.10.2027 | 31.10.2030 | NORGE_ER_SEKUNDÆRLAND | 2            | INAKTIV          | I_ARBEID                  | NO                    | SE                             | NO                  |
      | 4       | 01.11.2030 | 30.06.2033 | NORGE_ER_SEKUNDÆRLAND | 2            | INAKTIV          | I_ARBEID                  | NO                    | SE                             | NO                  |

    Og med utenlandsk periodebeløp
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 4       | 01.2023   | 09.2027   | 1            | 1493  | SEK         | MÅNEDLIG  | SE              |
      | 4       | 10.2027   | 10.2030   | 1            | 1493  | SEK         | MÅNEDLIG  | SE              |
      | 4       | 11.2030   | 06.2033   | 1            | 1493  | SEK         | MÅNEDLIG  | SE              |

      | 4       | 01.2023   | 09.2027   | 2            | 1493  | SEK         | MÅNEDLIG  | SE              |
      | 4       | 10.2027   | 10.2030   | 2            | 1493  | SEK         | MÅNEDLIG  | SE              |
      | 4       | 11.2030   | 06.2033   | 2            | 1493  | SEK         | MÅNEDLIG  | SE              |

    Og med valutakurser
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 4       | 01.01.2023 | 31.05.2024 | 1            | 2023-12-18     | SEK         | 1.0182593091 | MANUELL        |
      | 4       | 01.06.2024 | 30.06.2024 | 1            | 2024-05-31     | SEK         | 0.9966727957 | AUTOMATISK     |
      | 4       | 01.07.2024 | 31.07.2024 | 1            | 2024-06-28     | SEK         | 1.0032571856 | AUTOMATISK     |
      | 4       | 01.08.2024 | 31.08.2024 | 1            | 2024-07-31     | SEK         | 1.0176533907 | AUTOMATISK     |
      | 4       | 01.09.2024 | 30.09.2024 | 1            | 2024-08-30     | SEK         | 1.0288033170 | AUTOMATISK     |
      | 4       | 01.10.2024 | 31.10.2024 | 1            | 2024-09-30     | SEK         | 1.0411061947 | AUTOMATISK     |
      | 4       | 01.11.2024 | 30.11.2024 | 1            | 2024-10-31     | SEK         | 1.0264556178 | AUTOMATISK     |
      | 4       | 01.12.2024 | 31.12.2024 | 1            | 2024-11-29     | SEK         | 1.0141083521 | AUTOMATISK     |
      | 4       | 01.01.2025 | 30.09.2027 | 1            | 2024-12-31     | SEK         | 1.0293219304 | AUTOMATISK     |
      | 4       | 01.10.2027 | 31.10.2030 | 1            | 2023-12-18     | SEK         | 1.0182593091 | MANUELL        |
      | 4       | 01.11.2030 | 30.06.2033 | 1            | 2023-12-18     | SEK         | 1.0182593091 | MANUELL        |

      | 4       | 01.01.2023 | 31.05.2024 | 2            | 2023-12-18     | SEK         | 1.0182593091 | MANUELL        |
      | 4       | 01.06.2024 | 30.06.2024 | 2            | 2024-05-31     | SEK         | 0.9966727957 | AUTOMATISK     |
      | 4       | 01.07.2024 | 31.07.2024 | 2            | 2024-06-28     | SEK         | 1.0032571856 | AUTOMATISK     |
      | 4       | 01.08.2024 | 31.08.2024 | 2            | 2024-07-31     | SEK         | 1.0176533907 | AUTOMATISK     |
      | 4       | 01.09.2024 | 30.09.2024 | 2            | 2024-08-30     | SEK         | 1.0288033170 | AUTOMATISK     |
      | 4       | 01.10.2024 | 31.10.2024 | 2            | 2024-09-30     | SEK         | 1.0411061947 | AUTOMATISK     |
      | 4       | 01.11.2024 | 30.11.2024 | 2            | 2024-10-31     | SEK         | 1.0264556178 | AUTOMATISK     |
      | 4       | 01.12.2024 | 30.09.2027 | 2            | 2024-11-29     | SEK         | 1.0141083521 | AUTOMATISK     |
      | 4       | 01.10.2027 | 31.10.2030 | 2            | 2023-12-18     | SEK         | 1.0182593091 | MANUELL        |
      | 4       | 01.11.2030 | 30.06.2033 | 2            | 2023-12-18     | SEK         | 1.0182593091 | MANUELL        |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.08.2023 | 31.12.2023 | 2306  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 1            | 01.01.2024 | 31.05.2024 | 2506  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 1            | 01.06.2024 | 31.07.2024 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 1            | 01.08.2024 | 31.08.2024 | 2507  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 1            | 01.09.2024 | 30.06.2033 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |

      | 4       | 1            | 01.01.2023 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 4       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 4       | 1            | 01.07.2023 | 31.12.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 4       | 1            | 01.01.2024 | 31.05.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 4       | 1            | 01.06.2024 | 30.06.2024 | 22    | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 4       | 1            | 01.07.2024 | 31.07.2024 | 13    | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 4       | 1            | 01.08.2024 | 31.08.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 4       | 1            | 01.09.2024 | 30.09.2024 | 230   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 1            | 01.10.2024 | 31.10.2024 | 212   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 1            | 01.11.2024 | 30.11.2024 | 234   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 1            | 01.12.2024 | 31.12.2024 | 252   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 1            | 01.01.2025 | 30.09.2027 | 230   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 1            | 01.10.2027 | 30.06.2033 | 246   | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Når vi utfører vilkårsvurderingssteget for behandling 2

    Så forvent følgende valutakurser for behandling 2
      | BehandlingId | AktørId | Fra dato   | Til dato   | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2            | 4       | 01.01.2023 | 31.05.2024 | 2023-12-18     | SEK         | 1.0182593091 | MANUELL        |
      | 2            | 4       | 01.06.2024 | 30.06.2024 | 2024-05-31     | SEK         | 0.9966727957 | AUTOMATISK     |
      | 2            | 4       | 01.07.2024 | 31.07.2024 | 2024-06-28     | SEK         | 1.0032571856 | AUTOMATISK     |
      | 2            | 4       | 01.08.2024 | 31.08.2024 | 2024-07-31     | SEK         | 1.0176533907 | AUTOMATISK     |
      | 2            | 4       | 01.09.2024 | 30.09.2024 | 2024-08-30     | SEK         | 1.0288033170 | AUTOMATISK     |
      | 2            | 4       | 01.10.2024 | 31.10.2024 | 2024-09-30     | SEK         | 1.0411061947 | AUTOMATISK     |
      | 2            | 4       | 01.11.2024 | 30.11.2024 | 2024-10-31     | SEK         | 1.0264556178 | AUTOMATISK     |
      | 2            | 4       | 01.12.2024 | 31.12.2024 | 2024-11-29     | SEK         | 1.0141083521 | AUTOMATISK     |
