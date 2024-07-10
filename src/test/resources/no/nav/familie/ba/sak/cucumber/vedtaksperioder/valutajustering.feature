# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder med valutajustering

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | Nei                       | EØS                 | AVSLUTTET         |
      | 2            | 1        | 1                   | FORTSATT_OPPHØRT    | ÅRLIG_KONTROLL   | Nei                       | EØS                 | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 14.03.1989  |              |
      | 1            | 2       | BARN       | 02.07.2020  |              |
      | 2            | 1       | SØKER      | 14.03.1989  |              |
      | 2            | 2       | BARN       | 02.07.2020  |              |

  Scenario: Skal ikke generere vedtaksperioder med bare valutajustering
    Og dagens dato er 20.06.2024
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                              | 14.03.1989 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 15.03.2024 | 15.06.2024 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | LOVLIG_OPPHOLD   |                              | 02.07.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | GIFT_PARTNERSKAP |                              | 02.07.2020 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR      |                              | 02.07.2020 | 01.07.2038 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 02.07.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_NORGE_MED_SØKER   | 02.07.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                              | 14.03.1989 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 15.03.2024 | 15.06.2024 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | LOVLIG_OPPHOLD   |                              | 02.07.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | UNDER_18_ÅR      |                              | 02.07.2020 | 01.07.2038 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_NORGE_MED_SØKER   | 02.07.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 02.07.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | GIFT_PARTNERSKAP |                              | 02.07.2020 |            | OPPFYLT  | Nei                  |                      |                  |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.04.2024 | 30.06.2024 | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | DK                    | NO                             | DK                  |
      | 2       | 01.04.2024 | 30.06.2024 | NORGE_ER_SEKUNDÆRLAND | 2            | ARBEIDER         | I_ARBEID                  | DK                    | NO                             | DK                  |

    Og med utenlandsk periodebeløp
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2       | 04.2024   | 06.2024   | 1            | 700   | DKK         | MÅNEDLIG  | DK              |
      | 2       | 04.2024   | 04.2024   | 2            | 700   | DKK         | MÅNEDLIG  | DK              |
      | 2       | 05.2024   | 06.2024   | 2            | 800   | DKK         | MÅNEDLIG  | DK              |

    Og med valutakurser
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2       | 01.06.2024 | 30.06.2024 | 1            | 2024-05-31     | DKK         | 1.5261168016 | AUTOMATISK     |
      | 2       | 01.04.2024 | 30.04.2024 | 1            | 2024-03-27     | DKK         | 1.5663967177 | AUTOMATISK     |
      | 2       | 01.05.2024 | 31.05.2024 | 1            | 2024-04-30     | DKK         | 1.5841411582 | AUTOMATISK     |
      | 2       | 01.04.2024 | 30.04.2024 | 2            | 2024-03-27     | DKK         | 1.5663967177 | AUTOMATISK     |
      | 2       | 01.05.2024 | 31.05.2024 | 2            | 2024-04-30     | DKK         | 1.5841411582 | AUTOMATISK     |
      | 2       | 01.06.2024 | 30.06.2024 | 2            | 2024-05-31     | DKK         | 1.5261168016 | AUTOMATISK     |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.04.2024 | 30.04.2024 | 670   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.05.2024 | 31.05.2024 | 658   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.06.2024 | 30.06.2024 | 698   | ORDINÆR_BARNETRYGD | 100     | 1766 |

      | 2       | 2            | 01.04.2024 | 30.04.2024 | 670   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 2            | 01.05.2024 | 31.05.2024 | 499   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 2            | 01.06.2024 | 30.06.2024 | 546   | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent følgende vedtaksperioder for behandling 2
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar     |
      | 01.05.2024 | 30.06.2024 | Utbetaling         | Barn og søker |
      | 01.07.2024 |            | Opphør             | Barn og søker |


  Scenario: Årlig kontroll får riktige vedtaksperioder når automatiske valutajusteringer er eneste endring tilbake i tid
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak         |
      | 1            | 1        |                     | FORTSATT_INNVILGET  | MÅNEDLIG_VALUTAJUSTERING |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | ÅRLIG_KONTROLL           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 18.01.1975  |
      | 1            | 2       | BARN       | 16.07.2008  |
      | 2            | 1       | SØKER      | 18.01.1975  |
      | 2            | 2       | BARN       | 16.07.2008  |

    Og følgende dagens dato 09.07.2024

    Og lag personresultater for begrunnelse for behandling 1
    Og lag personresultater for begrunnelse for behandling 2

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD   |                              | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | GIFT_PARTNERSKAP |                              | 16.07.2008 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR      |                              | 16.07.2008 | 15.07.2026 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |


    Og legg til nye vilkårresultater for begrunnelse for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD   |                              | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | GIFT_PARTNERSKAP |                              | 16.07.2008 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR      |                              | 16.07.2008 | 15.07.2026 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |


    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.01.2022 | 28.02.2023 | 796   | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 825   | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 31.12.2023 | 1052  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 1            | 01.01.2024 | 31.05.2024 | 1252  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 1            | 01.06.2024 | 30.06.2026 | 1226  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 2            | 01.01.2022 | 31.12.2022 | 803   | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.01.2023 | 31.01.2023 | 792   | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.02.2023 | 28.02.2023 | 782   | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.03.2023 | 31.03.2023 | 809   | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.04.2023 | 30.04.2023 | 799   | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.05.2023 | 31.05.2023 | 789   | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.06.2023 | 30.06.2023 | 783   | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.07.2023 | 31.07.2023 | 1018  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 2            | 01.08.2023 | 31.08.2023 | 1031  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 2            | 01.09.2023 | 30.09.2023 | 1021  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 2            | 01.10.2023 | 31.10.2023 | 1029  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 2            | 01.11.2023 | 30.11.2023 | 1014  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 2            | 01.12.2023 | 31.12.2023 | 1017  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 2            | 01.01.2024 | 31.01.2024 | 1229  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 2            | 01.02.2024 | 29.02.2024 | 1227  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 2            | 01.03.2024 | 31.03.2024 | 1223  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 2            | 01.04.2024 | 30.04.2024 | 1218  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 2            | 01.05.2024 | 31.05.2024 | 1215  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 2            | 01.06.2024 | 30.06.2026 | 1226  | ORDINÆR_BARNETRYGD | 100     | 1510 |

    Og med kompetanser for begrunnelse
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.01.2022 |          | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | LV                             | LV                  |
      | 2       | 01.01.2022 |          | NORGE_ER_SEKUNDÆRLAND | 2            | ARBEIDER         | I_ARBEID                  | NO                    | LV                             | LV                  |

    Og med utenlandsk periodebeløp for begrunnelse
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2       | 01.2022   |           | 1            | 25    | EUR         | MÅNEDLIG  | LV              |
      | 2       | 01.2022   |           | 2            | 25    | EUR         | MÅNEDLIG  | LV              |

    Og med valutakurs for begrunnelse
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs    | Vurderingsform |
      | 2       | 01.01.2022 | 31.05.2024 | 1            | 2022-06-30     | EUR         | 10.3485 | MANUELL        |
      | 2       | 01.06.2024 | 30.06.2024 | 1            | 2024-05-31     | EUR         | 11.383  | AUTOMATISK     |
      | 2       | 01.07.2024 |            | 1            | 2024-06-28     | EUR         | 11.3965 | AUTOMATISK     |
      | 2       | 01.06.2024 | 30.06.2024 | 2            | 2024-05-31     | EUR         | 11.383  | AUTOMATISK     |
      | 2       | 01.07.2024 |            | 2            | 2024-06-28     | EUR         | 11.3965 | AUTOMATISK     |
      | 2       | 01.01.2022 | 31.12.2022 | 2            | 2022-06-01     | EUR         | 10.0438 | MANUELL        |
      | 2       | 01.01.2023 | 31.01.2023 | 2            | 2022-12-30     | EUR         | 10.5138 | AUTOMATISK     |
      | 2       | 01.02.2023 | 28.02.2023 | 2            | 2023-01-31     | EUR         | 10.9083 | AUTOMATISK     |
      | 2       | 01.03.2023 | 31.03.2023 | 2            | 2023-02-28     | EUR         | 10.9713 | AUTOMATISK     |
      | 2       | 01.04.2023 | 30.04.2023 | 2            | 2023-03-31     | EUR         | 11.394  | AUTOMATISK     |
      | 2       | 01.05.2023 | 31.05.2023 | 2            | 2023-04-28     | EUR         | 11.791  | AUTOMATISK     |
      | 2       | 01.06.2023 | 30.06.2023 | 2            | 2023-05-31     | EUR         | 12.0045 | AUTOMATISK     |
      | 2       | 01.07.2023 | 31.07.2023 | 2            | 2023-06-30     | EUR         | 11.704  | AUTOMATISK     |
      | 2       | 01.08.2023 | 31.08.2023 | 2            | 2023-07-31     | EUR         | 11.1805 | AUTOMATISK     |
      | 2       | 01.09.2023 | 30.09.2023 | 2            | 2023-08-31     | EUR         | 11.58   | AUTOMATISK     |
      | 2       | 01.10.2023 | 31.10.2023 | 2            | 2023-09-29     | EUR         | 11.2535 | AUTOMATISK     |
      | 2       | 01.11.2023 | 30.11.2023 | 2            | 2023-10-31     | EUR         | 11.8735 | AUTOMATISK     |
      | 2       | 01.12.2023 | 31.12.2023 | 2            | 2023-11-30     | EUR         | 11.72   | AUTOMATISK     |
      | 2       | 01.01.2024 | 31.01.2024 | 2            | 2023-12-29     | EUR         | 11.2405 | AUTOMATISK     |
      | 2       | 01.02.2024 | 29.02.2024 | 2            | 2024-01-31     | EUR         | 11.351  | AUTOMATISK     |
      | 2       | 01.03.2024 | 31.03.2024 | 2            | 2024-02-29     | EUR         | 11.492  | AUTOMATISK     |
      | 2       | 01.04.2024 | 30.04.2024 | 2            | 2024-03-27     | EUR         | 11.6825 | AUTOMATISK     |
      | 2       | 01.05.2024 | 31.05.2024 | 2            | 2024-04-30     | EUR         | 11.815  | AUTOMATISK     |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent følgende vedtaksperioder for behandling 2
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.01.2022 | 28.02.2023 | UTBETALING         |           |
      | 01.03.2023 | 30.06.2023 | UTBETALING         |           |
      | 01.07.2023 | 31.12.2023 | UTBETALING         |           |
      | 01.01.2024 | 30.06.2026 | UTBETALING         |           |
      | 01.07.2026 |            | OPPHØR             |           |

    Så forvent at endringstidspunktet er 01.01.2022 for behandling 2
