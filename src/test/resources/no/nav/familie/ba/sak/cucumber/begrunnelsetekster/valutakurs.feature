# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for valutakurs

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | ENDRET_UTBETALING   | SATSENDRING      | Ja                        | EØS                 |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | ÅRLIG_KONTROLL   | Nei                       | EØS                 |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 14.10.1987  |
      | 1            | 2       | BARN       | 10.10.2019  |
      | 2            | 1       | SØKER      | 14.10.1987  |
      | 2            | 2       | BARN       | 10.10.2019  |

  Scenario: Vis kompetansebegrunnelser dersom det er lagt til valutakurs for periode
    Og følgende dagens dato 24.10.2023
    Og lag personresultater for begrunnelse for behandling 1
    Og lag personresultater for begrunnelse for behandling 2

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 10.10.2019 |            | OPPFYLT  | Nei                  |                      |
      | 1       | LOVLIG_OPPHOLD   |                              | 10.10.2019 |            | OPPFYLT  | Nei                  |                      |

      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 10.10.2019 |            | OPPFYLT  | Nei                  |                      |
      | 2       | GIFT_PARTNERSKAP |                              | 10.10.2019 |            | OPPFYLT  | Nei                  |                      |
      | 2       | UNDER_18_ÅR      |                              | 10.10.2019 | 09.10.2037 | OPPFYLT  | Nei                  |                      |
      | 2       | LOVLIG_OPPHOLD   |                              | 10.10.2019 |            | OPPFYLT  | Nei                  |                      |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 10.10.2019 |            | OPPFYLT  | Nei                  |                      |

    Og legg til nye vilkårresultater for begrunnelse for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser |
      | 1       | LOVLIG_OPPHOLD   |                              | 10.10.2019 |            | OPPFYLT  | Nei                  |                      |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 10.10.2019 |            | OPPFYLT  | Nei                  |                      |

      | 2       | GIFT_PARTNERSKAP |                              | 10.10.2019 |            | OPPFYLT  | Nei                  |                      |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 10.10.2019 |            | OPPFYLT  | Nei                  |                      |
      | 2       | LOVLIG_OPPHOLD   |                              | 10.10.2019 |            | OPPFYLT  | Nei                  |                      |
      | 2       | UNDER_18_ÅR      |                              | 10.10.2019 | 09.10.2037 | OPPFYLT  | Nei                  |                      |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 10.10.2019 |            | OPPFYLT  | Nei                  |                      |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.11.2019 | 31.12.2019 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.01.2020 | 31.08.2020 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.09.2020 | 31.12.2020 | 206   | ORDINÆR_BARNETRYGD | 100     | 1354 |
      | 2       | 1            | 01.01.2021 | 31.08.2021 | 268   | ORDINÆR_BARNETRYGD | 100     | 1354 |
      | 2       | 1            | 01.09.2021 | 31.12.2021 | 568   | ORDINÆR_BARNETRYGD | 100     | 1654 |
      | 2       | 1            | 01.01.2022 | 28.02.2023 | 590   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 637   | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 2       | 1            | 01.07.2023 | 30.09.2025 | 680   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.10.2025 | 30.09.2037 | 224   | ORDINÆR_BARNETRYGD | 100     | 1310 |

      | 2       | 2            | 01.11.2019 | 31.12.2019 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.01.2020 | 31.08.2020 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.09.2020 | 31.12.2020 | 206   | ORDINÆR_BARNETRYGD | 100     | 1354 |
      | 2       | 2            | 01.01.2021 | 31.08.2021 | 268   | ORDINÆR_BARNETRYGD | 100     | 1354 |
      | 2       | 2            | 01.09.2021 | 31.12.2021 | 568   | ORDINÆR_BARNETRYGD | 100     | 1654 |
      | 2       | 2            | 01.01.2022 | 28.02.2023 | 553   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 600   | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 2       | 2            | 01.07.2023 | 30.09.2025 | 643   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 2            | 01.10.2025 | 30.09.2037 | 187   | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med kompetanser for begrunnelse
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.11.2019 |          | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |
      | 2       | 01.11.2019 |          | NORGE_ER_SEKUNDÆRLAND | 2            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |

    Og med valutakurs for begrunnelse
      | AktørId | Fra dato   | Til dato | BehandlingId | Valutakursdato | Valuta kode | Kurs |
      | 2       | 01.01.2022 |          | 2            | 30.12.2022     | PLN         | 2.24 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                    | Ugyldige begrunnelser |
      | 01.01.2022 | 28.02.2023 | UTBETALING         | EØS_FORORDNINGEN               | INNVILGET_TILLEGGSTEKST_VALUTAJUSTERING |                       |
      | 01.03.2023 | 30.06.2023 | UTBETALING         |                                | INNVILGET_SATSENDRING                   |                       |
      | 01.07.2023 | 30.09.2025 | UTBETALING         |                                | INNVILGET_SATSENDRING                   |                       |
      | 01.10.2025 | 30.09.2037 | UTBETALING         |                                |                                         |                       |
      | 01.10.2037 |            | OPPHØR             |                                |                                         |                       |
