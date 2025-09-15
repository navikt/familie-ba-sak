# language: no
# encoding: UTF-8

Egenskap: Behandlig med finnmarkstillegg

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status    |
      | 1        | NORMAL     | OPPRETTET |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 31.03.1990  |              |
      | 1            | 2       | BARN       | 08.11.2018  |              |
      | 1            | 3       | BARN       | 16.01.2023  |              |

  Scenario: Skal ikke splitte vedtaksperioder når søker flytter til finnmark men ikke barn
    Og dagens dato er 05.09.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 3       |
      | 1            | 2       |
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD                              |                              | 31.03.1990 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET                              |                              | 08.11.2018 | 06.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET                              | BOSATT_I_FINNMARK_NORD_TROMS | 07.08.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                              | 08.11.2018 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | GIFT_PARTNERSKAP                            |                              | 08.11.2018 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                                 |                              | 08.11.2018 | 07.11.2036 | OPPFYLT  | Nei                  |                      |                  |

      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER |                              | 16.01.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | GIFT_PARTNERSKAP                            |                              | 16.01.2023 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR                                 |                              | 16.01.2023 | 15.01.2041 | OPPFYLT  | Nei                  |                      |                  |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 1            | 01.12.2018 | 31.01.2023 | ETTERBETALING_3MND | 0       | 03.09.2025       |                             |
      | 3,2     | 1            | 01.02.2023 | 31.05.2025 | ETTERBETALING_3MND | 0       | 03.09.2025       |                             |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 2       | 1            | 01.12.2018 | 28.02.2019 | 0     | ORDINÆR_BARNETRYGD | 0       | 970  |
      | 2       | 1            | 01.03.2019 | 31.08.2020 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 2       | 1            | 01.09.2020 | 31.08.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1354 |
      | 2       | 1            | 01.09.2021 | 31.12.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1654 |
      | 2       | 1            | 01.01.2022 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 2       | 1            | 01.07.2023 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 1            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 1            | 01.06.2025 | 31.10.2036 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 1            | 01.02.2023 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 3       | 1            | 01.07.2023 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 3       | 1            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 3       | 1            | 01.06.2025 | 31.12.2040 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |

    Når vedtaksperiodene genereres for behandling 1


    Så forvent følgende vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.12.2018 | 31.01.2023 | OPPHØR             |           |
      | 01.02.2023 | 31.05.2025 | OPPHØR             |           |
      | 01.06.2025 | 31.10.2036 | UTBETALING         |           |
      | 01.11.2036 | 31.12.2040 | UTBETALING         |           |
      | 01.01.2041 |            | OPPHØR             |           |


  Scenario: Skal lage splitt i vedtaksperioder når det er andre utdypende vilkår enn finnmarkstillegg
    Og dagens dato er 05.09.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 3       |
      | 1            | 2       |
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår                                 | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD                              |                                                  | 31.03.1990 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET                              |                                                  | 08.11.2018 | 06.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET                              | VURDERT_MEDLEMSKAP, BOSATT_I_FINNMARK_NORD_TROMS | 07.08.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                                                  | 08.11.2018 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | GIFT_PARTNERSKAP                            |                                                  | 08.11.2018 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                                 |                                                  | 08.11.2018 | 07.11.2036 | OPPFYLT  | Nei                  |                      |                  |

      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER |                                                  | 16.01.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | GIFT_PARTNERSKAP                            |                                                  | 16.01.2023 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR                                 |                                                  | 16.01.2023 | 15.01.2041 | OPPFYLT  | Nei                  |                      |                  |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 1            | 01.12.2018 | 31.01.2023 | ETTERBETALING_3MND | 0       | 03.09.2025       |                             |
      | 3,2     | 1            | 01.02.2023 | 31.05.2025 | ETTERBETALING_3MND | 0       | 03.09.2025       |                             |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 2       | 1            | 01.12.2018 | 28.02.2019 | 0     | ORDINÆR_BARNETRYGD | 0       | 970  |
      | 2       | 1            | 01.03.2019 | 31.08.2020 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 2       | 1            | 01.09.2020 | 31.08.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1354 |
      | 2       | 1            | 01.09.2021 | 31.12.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1654 |
      | 2       | 1            | 01.01.2022 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 2       | 1            | 01.07.2023 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 1            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 1            | 01.06.2025 | 31.10.2036 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 1            | 01.02.2023 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 3       | 1            | 01.07.2023 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 3       | 1            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 3       | 1            | 01.06.2025 | 31.12.2040 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |

    Når vedtaksperiodene genereres for behandling 1


    Så forvent følgende vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.12.2018 | 31.01.2023 | OPPHØR             |           |
      | 01.02.2023 | 31.05.2025 | OPPHØR             |           |
      | 01.06.2025 | 31.08.2025 | UTBETALING         |           |
      | 01.09.2025 | 31.10.2036 | UTBETALING         |           |
      | 01.11.2036 | 31.12.2040 | UTBETALING         |           |
      | 01.01.2041 |            | OPPHØR             |           |


  Scenario: Skal splitte vedtaksperioder når søker flytter ut av finnmark men ikke barn
    Og dagens dato er 05.09.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 3       |
      | 1            | 2       |
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD                              |                              | 31.03.1990 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET                              | BOSATT_I_FINNMARK_NORD_TROMS | 08.11.2018 | 06.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET                              |                              | 07.08.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                              | 08.11.2018 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET                              | BOSATT_I_FINNMARK_NORD_TROMS | 08.11.2018 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | GIFT_PARTNERSKAP                            |                              | 08.11.2018 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                                 |                              | 08.11.2018 | 07.11.2036 | OPPFYLT  | Nei                  |                      |                  |

      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER |                              | 16.01.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOSATT_I_RIKET                              | BOSATT_I_FINNMARK_NORD_TROMS | 16.01.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | GIFT_PARTNERSKAP                            |                              | 16.01.2023 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR                                 |                              | 16.01.2023 | 15.01.2041 | OPPFYLT  | Nei                  |                      |                  |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 1            | 01.12.2018 | 31.01.2023 | ETTERBETALING_3MND | 0       | 03.09.2025       |                             |
      | 3,2     | 1            | 01.02.2023 | 31.05.2025 | ETTERBETALING_3MND | 0       | 03.09.2025       |                             |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 2       | 1            | 01.12.2018 | 28.02.2019 | 0     | ORDINÆR_BARNETRYGD | 0       | 970  |
      | 2       | 1            | 01.03.2019 | 31.08.2020 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 2       | 1            | 01.09.2020 | 31.08.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1354 |
      | 2       | 1            | 01.09.2021 | 31.12.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1654 |
      | 2       | 1            | 01.01.2022 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 2       | 1            | 01.07.2023 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 1            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 1            | 01.06.2025 | 31.10.2036 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 1            | 01.02.2023 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 3       | 1            | 01.07.2023 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 3       | 1            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 3       | 1            | 01.06.2025 | 31.12.2040 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |

    Når vedtaksperiodene genereres for behandling 1


    Så forvent følgende vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.12.2018 | 31.01.2023 | OPPHØR             |           |
      | 01.02.2023 | 31.05.2025 | OPPHØR             |           |
      | 01.06.2025 | 31.10.2036 | UTBETALING         |           |
      | 01.11.2036 | 31.12.2040 | UTBETALING         |           |
      | 01.01.2041 |            | OPPHØR             |           |

  Scenario: Skal ikke splitte vedtaksperioder når søker flytter ut av Finnmark i samme måned som barn flytter inn
    Og dagens dato er 05.09.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 3       |
      | 1            | 2       |
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD                              |                              | 31.03.1990 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET                              |                              | 08.11.2018 | 06.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET                              | BOSATT_I_FINNMARK_NORD_TROMS | 31.08.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                              | 08.11.2018 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                               | BOSATT_I_FINNMARK_NORD_TROMS | 08.11.2018 | 01.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | GIFT_PARTNERSKAP                            |                              | 08.11.2018 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                                 |                              | 08.11.2018 | 07.11.2036 | OPPFYLT  | Nei                  |                      |                  |

      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER |                              | 16.01.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | GIFT_PARTNERSKAP                            |                              | 16.01.2023 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR                                 |                              | 16.01.2023 | 15.01.2041 | OPPFYLT  | Nei                  |                      |                  |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 1            | 01.12.2018 | 31.01.2023 | ETTERBETALING_3MND | 0       | 03.09.2025       |                             |
      | 3,2     | 1            | 01.02.2023 | 31.05.2025 | ETTERBETALING_3MND | 0       | 03.09.2025       |                             |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 2       | 1            | 01.12.2018 | 28.02.2019 | 0     | ORDINÆR_BARNETRYGD | 0       | 970  |
      | 2       | 1            | 01.03.2019 | 31.08.2020 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 2       | 1            | 01.09.2020 | 31.08.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1354 |
      | 2       | 1            | 01.09.2021 | 31.12.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1654 |
      | 2       | 1            | 01.01.2022 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 2       | 1            | 01.07.2023 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 1            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 1            | 01.06.2025 | 31.10.2036 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 1            | 01.02.2023 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 3       | 1            | 01.07.2023 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 3       | 1            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 3       | 1            | 01.06.2025 | 31.12.2040 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |

    Når vedtaksperiodene genereres for behandling 1


    Så forvent følgende vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.12.2018 | 31.01.2023 | OPPHØR             |           |
      | 01.02.2023 | 31.05.2025 | OPPHØR             |           |
      | 01.06.2025 | 31.10.2036 | UTBETALING         |           |
      | 01.11.2036 | 31.12.2040 | UTBETALING         |           |
      | 01.01.2041 |            | OPPHØR             |           |