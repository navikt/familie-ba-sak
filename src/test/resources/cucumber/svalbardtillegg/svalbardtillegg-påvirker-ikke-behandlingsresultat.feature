# language: no
# encoding: UTF-8

Egenskap: Behandling med svalbardtillegg på én person skal ikke påvirke behandlingsresultat

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | NYE_OPPLYSNINGER | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 09.07.1990  |              |
      | 1            | 2       | BARN       | 22.05.2019  |              |
      | 2            | 1       | SØKER      | 09.07.1990  |              |
      | 2            | 2       | BARN       | 22.05.2019  |              |

  Scenario: Kun søker flytter til Svalbard skal ikke gi behandlingsresultat ENDRET
    Og dagens dato er 15.09.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD                              |                  | 09.07.1990 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET                              |                  | 22.05.2019 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 22.05.2019 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | GIFT_PARTNERSKAP                            |                  | 22.05.2019 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                                 |                  | 22.05.2019 | 21.05.2037 | OPPFYLT  | Nei                  |                      |                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                      | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD                              |                              | 09.07.1990 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET                              |                              | 22.05.2019 | 08.09.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET                              | BOSATT_PÅ_SVALBARD | 09.09.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | GIFT_PARTNERSKAP                            |                              | 22.05.2019 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                              | 22.05.2019 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | UNDER_18_ÅR                                 |                              | 22.05.2019 | 21.05.2037 | OPPFYLT  | Nei                  |                      |                  |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 1            | 01.06.2019 | 31.05.2025 | ETTERBETALING_3MND | 0       | 09.09.2025       |                             |
      | 2       | 2            | 01.06.2019 | 31.05.2025 | ETTERBETALING_3MND | 0       | 09.09.2025       |                             |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.06.2019 | 31.08.2020 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 2       | 1            | 01.09.2020 | 31.08.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1354 |
      | 2       | 1            | 01.09.2021 | 31.12.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1654 |
      | 2       | 1            | 01.01.2022 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 2       | 1            | 01.07.2023 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 1            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 1            | 01.06.2025 | 30.04.2037 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |

      | 2       | 2            | 01.06.2019 | 31.08.2020 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 2       | 2            | 01.09.2020 | 31.08.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1354 |
      | 2       | 2            | 01.09.2021 | 31.12.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1654 |
      | 2       | 2            | 01.01.2022 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 2       | 2            | 01.07.2023 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 2            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 2            | 01.06.2025 | 30.04.2037 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |

    Når vedtaksperiodene genereres for behandling 2

    Og når behandlingsresultatet er utledet for behandling 2

    Så forvent at behandlingsresultatet er FORTSATT_INNVILGET på behandling 2


  Scenario: Kun søker flytter til Svalbard, men det er også endring i et annet utdypende vilkår skal gi behandlingsresultat ENDRET_UTEN_UTBETALING
    Og dagens dato er 15.09.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat |
      | 1       | LOVLIG_OPPHOLD                              |                  | 09.07.1990 |            | OPPFYLT  |
      | 1       | BOSATT_I_RIKET                              |                  | 22.05.2019 |            | OPPFYLT  |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 22.05.2019 |            | OPPFYLT  |
      | 2       | GIFT_PARTNERSKAP                            |                  | 22.05.2019 |            | OPPFYLT  |
      | 2       | UNDER_18_ÅR                                 |                  | 22.05.2019 | 21.05.2037 | OPPFYLT  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                      | Utdypende vilkår                                 | Fra dato   | Til dato   | Resultat |
      | 1       | LOVLIG_OPPHOLD                              |                                                  | 09.07.1990 |            | OPPFYLT  |
      | 1       | BOSATT_I_RIKET                              |                                                  | 22.05.2019 | 08.09.2025 | OPPFYLT  |
      | 1       | BOSATT_I_RIKET                              | BOSATT_PÅ_SVALBARD, VURDERT_MEDLEMSKAP | 09.09.2025 |            | OPPFYLT  |

      | 2       | GIFT_PARTNERSKAP                            |                                                  | 22.05.2019 |            | OPPFYLT  |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                                                  | 22.05.2019 |            | OPPFYLT  |
      | 2       | UNDER_18_ÅR                                 |                                                  | 22.05.2019 | 21.05.2037 | OPPFYLT  |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 1            | 01.06.2019 | 31.05.2025 | ETTERBETALING_3MND | 0       | 09.09.2025       |                             |
      | 2       | 2            | 01.06.2019 | 31.05.2025 | ETTERBETALING_3MND | 0       | 09.09.2025       |                             |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.06.2019 | 31.08.2020 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 2       | 1            | 01.09.2020 | 31.08.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1354 |
      | 2       | 1            | 01.09.2021 | 31.12.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1654 |
      | 2       | 1            | 01.01.2022 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 2       | 1            | 01.07.2023 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 1            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 1            | 01.06.2025 | 30.04.2037 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |

      | 2       | 2            | 01.06.2019 | 31.08.2020 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 2       | 2            | 01.09.2020 | 31.08.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1354 |
      | 2       | 2            | 01.09.2021 | 31.12.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1654 |
      | 2       | 2            | 01.01.2022 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 2       | 2            | 01.07.2023 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 2            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 2            | 01.06.2025 | 30.04.2037 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |

    Når vedtaksperiodene genereres for behandling 2

    Og når behandlingsresultatet er utledet for behandling 2

    Så forvent at behandlingsresultatet er ENDRET_UTBETALING på behandling 2

  Scenario: Søker og barn flytter til Svalbard skal gi behandlingsresultat ENDRET
    Og dagens dato er 15.09.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat |
      | 1       | LOVLIG_OPPHOLD                              |                  | 09.07.1990 |            | OPPFYLT  |
      | 1       | BOSATT_I_RIKET                              |                  | 22.05.2019 |            | OPPFYLT  |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 22.05.2019 |            | OPPFYLT  |
      | 2       | GIFT_PARTNERSKAP                            |                  | 22.05.2019 |            | OPPFYLT  |
      | 2       | UNDER_18_ÅR                                 |                  | 22.05.2019 | 21.05.2037 | OPPFYLT  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                        | Utdypende vilkår             | Fra dato   | Til dato   | Resultat |
      | 1       | LOVLIG_OPPHOLD                |                              | 09.07.1990 |            | OPPFYLT  |
      | 1       | BOSATT_I_RIKET                |                              | 22.05.2019 | 08.09.2025 | OPPFYLT  |
      | 1       | BOSATT_I_RIKET                | BOSATT_PÅ_SVALBARD | 09.09.2025 |            | OPPFYLT  |
      | 2       | GIFT_PARTNERSKAP              |                              | 22.05.2019 |            | OPPFYLT  |
      | 2       | BOR_MED_SØKER, LOVLIG_OPPHOLD |                              | 22.05.2019 |            | OPPFYLT  |
      | 2       | BOSATT_I_RIKET                |                              | 22.05.2019 | 08.09.2025 | OPPFYLT  |
      | 2       | BOSATT_I_RIKET                | BOSATT_PÅ_SVALBARD | 09.09.2025 |            | OPPFYLT  |
      | 2       | UNDER_18_ÅR                   |                              | 22.05.2019 | 21.05.2037 | OPPFYLT  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.06.2019 | 31.08.2020 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 2       | 1            | 01.09.2020 | 31.08.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1354 |
      | 2       | 1            | 01.09.2021 | 31.12.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1654 |
      | 2       | 1            | 01.01.2022 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 2       | 1            | 01.07.2023 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 1            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 1            | 01.06.2025 | 30.04.2037 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |

      | 2       | 2            | 01.06.2019 | 31.08.2020 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 2       | 2            | 01.09.2020 | 31.08.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1354 |
      | 2       | 2            | 01.09.2021 | 31.12.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1654 |
      | 2       | 2            | 01.01.2022 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 2       | 2            | 01.07.2023 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 2            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 2            | 01.06.2025 | 31.09.2025 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 2       | 2            | 01.10.2025 | 30.04.2037 | 2468  | ORDINÆR_BARNETRYGD | 100     | 1968 |

    Når vedtaksperiodene genereres for behandling 2

    Og når behandlingsresultatet er utledet for behandling 2

    Så forvent at behandlingsresultatet er ENDRET_UTBETALING på behandling 2


  Scenario: Endring i fom og tom på på vilkårresultat skal lage splitt
    Og dagens dato er 15.09.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat |
      | 1       | LOVLIG_OPPHOLD                              |                  | 09.07.1990 |            | OPPFYLT  |
      | 1       | BOSATT_I_RIKET                              |                  | 22.05.2019 |            | OPPFYLT  |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 22.05.2019 |            | OPPFYLT  |
      | 2       | GIFT_PARTNERSKAP                            |                  | 22.05.2019 |            | OPPFYLT  |
      | 2       | UNDER_18_ÅR                                 |                  | 22.05.2019 | 21.05.2037 | OPPFYLT  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat |
      | 1       | LOVLIG_OPPHOLD                |                  | 09.07.1990 |            | OPPFYLT  |
      | 1       | BOSATT_I_RIKET                |                  | 22.05.2019 | 08.09.2025 | OPPFYLT  |
      | 1       | BOSATT_I_RIKET                |                  | 09.09.2025 |            | OPPFYLT  |
      | 2       | GIFT_PARTNERSKAP              |                  | 22.05.2019 |            | OPPFYLT  |
      | 2       | BOR_MED_SØKER, LOVLIG_OPPHOLD |                  | 22.05.2019 |            | OPPFYLT  |
      | 2       | BOSATT_I_RIKET                |                  | 22.05.2019 |            | OPPFYLT  |
      | 2       | UNDER_18_ÅR                   |                  | 22.05.2019 | 21.05.2037 | OPPFYLT  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.06.2019 | 31.08.2020 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 2       | 1            | 01.09.2020 | 31.08.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1354 |
      | 2       | 1            | 01.09.2021 | 31.12.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1654 |
      | 2       | 1            | 01.01.2022 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 2       | 1            | 01.07.2023 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 1            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 1            | 01.06.2025 | 30.04.2037 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |

      | 2       | 2            | 01.06.2019 | 31.08.2020 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 2       | 2            | 01.09.2020 | 31.08.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1354 |
      | 2       | 2            | 01.09.2021 | 31.12.2021 | 0     | ORDINÆR_BARNETRYGD | 0       | 1654 |
      | 2       | 2            | 01.01.2022 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1676 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 2       | 2            | 01.07.2023 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 2            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 2            | 01.06.2025 | 30.04.2037 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |

    Når vedtaksperiodene genereres for behandling 2

    Og når behandlingsresultatet er utledet for behandling 2

    Så forvent at behandlingsresultatet er ENDRET_UTBETALING på behandling 2