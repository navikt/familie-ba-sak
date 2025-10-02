# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for etter endret utbetaling, en mor med ett barn

  Bakgrunn:
    Gitt følgende behandlinger
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 13.04.2020  |

  Scenario: Begrunnelse etter endret utbetaling delt bosted
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 13.04.2020 | 03.01.2021 | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 04.01.2021 | 15.01.2022 | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 16.01.2022 |            | Oppfylt  |

    Og med endrede utbetalinger
      | AktørId | Fra dato   | Til dato   | BehandlingId | Årsak       | Prosent | Avtaletidspunkt delt bosted |
      | 3456    | 01.05.2020 | 31.01.2021 | 1            | DELT_BOSTED | 0       | 02.02.2020                  |
      | 3456    | 01.02.2021 | 31.01.2022 | 1            | DELT_BOSTED | 100     | 02.02.2021                  |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId | Prosent |
      | 3456    | 01.05.2020 | 31.01.2021 | 0     | 1            | 0       |
      | 3456    | 01.02.2021 | 31.01.2022 | 1354  | 1            | 100     |
      | 3456    | 01.02.2022 | 31.03.2038 | 1354  | 1            | 100     |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Gyldige begrunnelser                           | Ugyldige begrunnelser                          | Kommentar                                          |
      | 01.05.2020 | 31.01.2021 | UTBETALING         |                                                |                                                | Ingen etter endret utbetalingsbegrunnelse skal med |
      | 01.02.2021 | 31.01.2022 | UTBETALING         |                                                | ETTER_ENDRET_UTBETALING_HAR_AVTALE_DELT_BOSTED |                                                    |
      | 01.02.2022 | 31.03.2038 | UTBETALING         | ETTER_ENDRET_UTBETALING_HAR_AVTALE_DELT_BOSTED |                                                |                                                    |
      | 01.04.2038 |            | OPPHØR             | OPPHØR_UNDER_18_ÅR                             |                                                |                                                    |

  Scenario: Begrunnelse etter endret utbetaling allerede utbetalt
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2020 |            | Oppfylt  |

    Og med endrede utbetalinger
      | AktørId | Fra dato   | Til dato   | BehandlingId | Årsak             | Prosent |
      | 3456    | 01.05.2020 | 31.01.2021 | 1            | ETTERBETALING_3ÅR | 0       |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId | Prosent |
      | 3456    | 01.05.2020 | 31.01.2021 | 0     | 1            | 0       |
      | 3456    | 01.02.2021 | 31.03.2038 | 1354  | 1            | 100     |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Gyldige begrunnelser                          | Ugyldige begrunnelser |
      | 01.05.2020 | 31.01.2021 | OPPHØR             |                                               |                       |
      | 01.02.2021 | 31.03.2038 | UTBETALING         | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_AAR |                       |
      | 01.04.2038 |            | OPPHØR             | OPPHØR_UNDER_18_ÅR                            |                       |

  Scenario: Vilkår som ble utgjørende i forrige periode men ikke blir begrunnet grunnet endret utbetaling skal kunne begrunnes neste periode

    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status    |
      | 1        | NORMAL     | AVSLUTTET |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat        | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | AVSLÅTT                    | SØKNAD           | Nei                       | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | DELVIS_INNVILGET_OG_ENDRET | SØKNAD           | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 18.02.1990  |              |
      | 1            | 2       | BARN       | 27.07.2016  |              |
      | 1            | 3       | BARN       | 15.01.2019  |              |
      | 1            | 4       | BARN       | 07.07.2020  |              |
      | 1            | 5       | BARN       | 28.06.2023  |              |
      | 2            | 1       | SØKER      | 18.02.1990  |              |
      | 2            | 2       | BARN       | 27.07.2016  |              |
      | 2            | 3       | BARN       | 15.01.2019  |              |
      | 2            | 4       | BARN       | 07.07.2020  |              |
      | 2            | 5       | BARN       | 28.06.2023  |              |

    Og dagens dato er 11.05.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 2            | 5       |
      | 2            | 4       |
      | 2            | 3       |
      | 2            | 2       |
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser        | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD               |                  | 02.08.2019 |            | OPPFYLT      | Nei                  |                             | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET               |                  | 25.11.2020 |            | IKKE_OPPFYLT | Ja                   | AVSLAG_BOSATT_I_RIKET_SØKER | NASJONALE_REGLER |

      | 2       | GIFT_PARTNERSKAP             |                  | 27.07.2016 |            | OPPFYLT      | Nei                  |                             |                  |
      | 2       | BOR_MED_SØKER                |                  | 27.07.2016 |            | OPPFYLT      | Nei                  |                             | NASJONALE_REGLER |
      | 2       | UNDER_18_ÅR                  |                  | 27.07.2016 | 26.07.2034 | OPPFYLT      | Nei                  |                             |                  |
      | 2       | LOVLIG_OPPHOLD               |                  | 02.08.2019 |            | OPPFYLT      | Nei                  |                             | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET               |                  | 25.11.2020 |            | IKKE_OPPFYLT | Ja                   | AVSLAG_BOSATT_I_RIKET       | NASJONALE_REGLER |

      | 3       | LOVLIG_OPPHOLD,BOR_MED_SØKER |                  | 15.01.2019 |            | OPPFYLT      | Nei                  |                             | NASJONALE_REGLER |
      | 3       | UNDER_18_ÅR                  |                  | 15.01.2019 | 14.01.2037 | OPPFYLT      | Nei                  |                             |                  |
      | 3       | GIFT_PARTNERSKAP             |                  | 15.01.2019 |            | OPPFYLT      | Nei                  |                             |                  |
      | 3       | BOSATT_I_RIKET               |                  | 25.11.2020 |            | IKKE_OPPFYLT | Ja                   | AVSLAG_BOSATT_I_RIKET       | NASJONALE_REGLER |

      | 4       | GIFT_PARTNERSKAP             |                  | 07.07.2020 |            | OPPFYLT      | Nei                  |                             |                  |
      | 4       | LOVLIG_OPPHOLD,BOR_MED_SØKER |                  | 07.07.2020 |            | OPPFYLT      | Nei                  |                             | NASJONALE_REGLER |
      | 4       | UNDER_18_ÅR                  |                  | 07.07.2020 | 06.07.2038 | OPPFYLT      | Nei                  |                             |                  |
      | 4       | BOSATT_I_RIKET               |                  | 25.11.2020 |            | IKKE_OPPFYLT | Ja                   | AVSLAG_BOSATT_I_RIKET       | NASJONALE_REGLER |

      | 5       | LOVLIG_OPPHOLD,BOR_MED_SØKER |                  | 28.06.2023 |            | OPPFYLT      | Nei                  |                             | NASJONALE_REGLER |
      | 5       | BOSATT_I_RIKET               |                  | 28.06.2023 |            | IKKE_OPPFYLT | Ja                   | AVSLAG_BOSATT_I_RIKET       | NASJONALE_REGLER |
      | 5       | UNDER_18_ÅR                  |                  | 28.06.2023 | 27.06.2041 | OPPFYLT      | Nei                  |                             |                  |
      | 5       | GIFT_PARTNERSKAP             |                  | 28.06.2023 |            | OPPFYLT      | Nei                  |                             |                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår   | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD               |                    | 02.08.2019 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET               | VURDERT_MEDLEMSKAP | 16.08.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | UNDER_18_ÅR                  |                    | 27.07.2016 | 26.07.2034 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER                |                    | 27.07.2016 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | GIFT_PARTNERSKAP             |                    | 27.07.2016 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | LOVLIG_OPPHOLD               |                    | 02.08.2019 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET               | VURDERT_MEDLEMSKAP | 16.08.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | BOR_MED_SØKER,LOVLIG_OPPHOLD |                    | 15.01.2019 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | UNDER_18_ÅR                  |                    | 15.01.2019 | 14.01.2037 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP             |                    | 15.01.2019 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOSATT_I_RIKET               | VURDERT_MEDLEMSKAP | 16.08.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 4       | GIFT_PARTNERSKAP             |                    | 07.07.2020 |            | OPPFYLT  | Nei                  |                      |                  |
      | 4       | LOVLIG_OPPHOLD,BOR_MED_SØKER |                    | 07.07.2020 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | UNDER_18_ÅR                  |                    | 07.07.2020 | 06.07.2038 | OPPFYLT  | Nei                  |                      |                  |
      | 4       | BOSATT_I_RIKET               | VURDERT_MEDLEMSKAP | 16.08.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 5       | UNDER_18_ÅR                  |                    | 28.06.2023 | 27.06.2041 | OPPFYLT  | Nei                  |                      |                  |
      | 5       | GIFT_PARTNERSKAP             |                    | 28.06.2023 |            | OPPFYLT  | Nei                  |                      |                  |
      | 5       | LOVLIG_OPPHOLD,BOR_MED_SØKER |                    | 28.06.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 5       | BOSATT_I_RIKET               | VURDERT_MEDLEMSKAP | 16.08.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med endrede utbetalinger
      | AktørId    | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2, 3, 4, 5 | 2            | 01.09.2023 | 30.11.2024 | ETTERBETALING_3MND | 0       | 20.03.2025       |                             |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 2       | 2            | 01.09.2023 | 30.11.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1310 |
      | 2       | 2            | 01.12.2024 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 2            | 01.05.2025 | 30.06.2034 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 2            | 01.09.2023 | 30.11.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 3       | 2            | 01.12.2024 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 2            | 01.05.2025 | 31.12.2036 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 4       | 2            | 01.09.2023 | 30.11.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 4       | 2            | 01.12.2024 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 2            | 01.05.2025 | 30.06.2038 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 5       | 2            | 01.09.2023 | 30.11.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 5       | 2            | 01.12.2024 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 5       | 2            | 01.05.2025 | 31.05.2041 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                 | Ugyldige begrunnelser |
      | 01.09.2023 | 30.11.2024 | OPPHØR             |                                |                                      |                       |
      | 01.12.2024 | 30.04.2025 | UTBETALING         |                                | INNVILGET_HELE_FAMILIEN_TRYGDEAVTALE |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                 | Eøsbegrunnelser | Fritekster |
      | 01.12.2024 | 30.04.2025 | INNVILGET_HELE_FAMILIEN_TRYGDEAVTALE |                 |            |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.12.2024 til 30.04.2025
      | Begrunnelse                          | Type     | Gjelder søker | Barnas fødselsdatoer                     | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | INNVILGET_HELE_FAMILIEN_TRYGDEAVTALE | STANDARD |               | 27.07.16, 15.01.19, 07.07.20 og 28.06.23 | 4           | november 2024                        |         | 7 064 |                  |                         |                             |
