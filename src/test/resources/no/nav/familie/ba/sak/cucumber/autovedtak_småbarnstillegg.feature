# language: no
# encoding: UTF-8

Egenskap: Småbarnstillegg autovedtak

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 2            | 1        |                     | ENDRET_UTBETALING   | SMÅBARNSTILLEGG  | Ja                        | NASJONAL            | AVSLUTTET         |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 2            | 1       | SØKER      | 11.02.1994  |              |
      | 2            | 2       | BARN       | 29.07.2021  |              |

  Scenario: Skal kjøre autovedtak småbarnstillegg automatisk dersom vi endrer overgangsstønaden fram i tid
    Og dagens dato er 27.11.2023
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 01.04.2022 | 31.12.2024 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD                          |                  | 01.04.2022 |            | OPPFYLT  | Nei                  |                      |                  |

      | 2       | UNDER_18_ÅR                                 |                  | 29.07.2021 | 28.07.2039 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP                            |                  | 29.07.2021 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER,LOVLIG_OPPHOLD |                  | 01.04.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 2            | 01.05.2022 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 2            | 01.05.2022 | 28.02.2023 | 660   | SMÅBARNSTILLEGG    | 100     | 660  |
      | 1       | 2            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 1       | 2            | 01.03.2023 | 30.06.2023 | 678   | SMÅBARNSTILLEGG    | 100     | 678  |
      | 1       | 2            | 01.07.2023 | 31.01.2024 | 696   | SMÅBARNSTILLEGG    | 100     | 696  |
      | 1       | 2            | 01.07.2023 | 30.11.2024 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 2            | 01.05.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 2       | 2            | 01.07.2023 | 30.12.2024 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Og med overgangsstønad
      | BehandlingId | AktørId | Fra dato   | Til dato   |
      | 2            | 1       | 01.05.2022 | 31.01.2024 |

    Når vi lager automatisk behandling med id 3 på fagsak 1 på grunn av nye overgangsstønadsperioder
      | Fra dato   | Til dato   |
      | 01.05.2022 | 30.06.2024 |

    Så forvent følgende andeler tilkjent ytelse for behandling 3
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 3            | 01.05.2022 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 3            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 1       | 3            | 01.05.2022 | 28.02.2023 | 660   | SMÅBARNSTILLEGG    | 100     | 660  |
      | 1       | 3            | 01.03.2023 | 30.06.2023 | 678   | SMÅBARNSTILLEGG    | 100     | 678  |
      | 1       | 3            | 01.07.2023 | 30.06.2024 | 696   | SMÅBARNSTILLEGG    | 100     | 696  |
      | 1       | 3            | 01.07.2023 | 30.12.2024 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 3            | 01.05.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 3            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 2       | 3            | 01.07.2023 | 30.12.2024 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |

    # Forventer at det ikke skal være noen brevperioder
    Så forvent følgende brevperioder for behandling 3
      | Brevperiodetype | Fra dato | Til dato | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |

  Scenario: Skal sende autovedtak småbarnstillegg til manuell behandling dersom noe endrer seg tilbake i tid
    Og dagens dato er 27.11.2023
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 01.04.2022 | 31.12.2024 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD                          |                  | 01.04.2022 |            | OPPFYLT  | Nei                  |                      |                  |

      | 2       | UNDER_18_ÅR                                 |                  | 29.07.2021 | 28.07.2039 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP                            |                  | 29.07.2021 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER,LOVLIG_OPPHOLD |                  | 01.04.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 2            | 01.05.2022 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 2            | 01.05.2022 | 28.02.2023 | 660   | SMÅBARNSTILLEGG    | 100     | 660  |
      | 1       | 2            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 1       | 2            | 01.03.2023 | 30.06.2023 | 678   | SMÅBARNSTILLEGG    | 100     | 678  |
      | 1       | 2            | 01.07.2023 | 31.01.2024 | 696   | SMÅBARNSTILLEGG    | 100     | 696  |
      | 1       | 2            | 01.07.2023 | 30.11.2024 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |

    Og med overgangsstønad
      | BehandlingId | AktørId | Fra dato   | Til dato   |
      | 2            | 1       | 01.05.2022 | 31.01.2024 |

    Og med dødsfall
      | AktørId | Dødsfalldato |
      | 1       | 27.11.2023   |

    Når vi lager automatisk behandling med id 3 på fagsak 1 på grunn av nye overgangsstønadsperioder
      | Fra dato   | Til dato   |
      | 01.05.2022 | 30.06.2024 |

    Så forvent disse behandlingene
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak                   | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus | Behandlingstype | Behandlingssteg     | Underkategori |
      | 3            | 1        | 2                   | ENDRET_UTBETALING   | SMÅBARNSTILLEGG_ENDRING_FRAM_I_TID | Nei                       | NASJONAL            | UTREDES           | Revurdering     | BEHANDLINGSRESULTAT | UTVIDET       |
