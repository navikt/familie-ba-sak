# language: no
# encoding: UTF-8

Egenskap: Automatisk behandling for ny klassekode for utvidet barnetrygd

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status    |
      | 1        | NORMAL     | OPPRETTET |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | Behandlingsårsak | Behandlingskategori | Underkategori | Behandlingsresultat | Behandlingsstatus | Behandlingssteg      |
      | 1            | 1        | SØKNAD           | NASJONAL            | UTVIDET       | INNVILGET           | AVSLUTTET         | BEHANDLING_AVSLUTTET |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 24.08.1989  |              |
      | 1            | 2       | BARN       | 13.06.2023  |              |

  Scenario: skal gjennomføres for fagsak som har løpende barnetrygd og ikke er oppdatert

    Og med følgende feature toggles
      | BehandlingId | FeatureToggleId                                                | Er togglet på |
      | 1            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Nei           |
      | 2            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Ja            |

    Og dagens dato er 15.11.2024
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                       | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD                                | 24.08.1989 |            | OPPFYLT  | Nei                  |
      | 1       | UTVIDET_BARNETRYGD                                           | 13.06.2023 |            | OPPFYLT  | Nei                  |

      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER,GIFT_PARTNERSKAP | 13.06.2023 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                                  | 13.06.2023 | 12.06.2041 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.07.2023 | 31.05.2041 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 1            | 01.07.2023 | 31.05.2041 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Når vi lager automatisk behandling på fagsak 1 med årsak OPPDATER_UTVIDET_KLASSEKODE

    Så forvent disse behandlingene
      | BehandlingId | FagsakId | Behandlingstype       | Behandlingsårsak            | Behandlingskategori | Underkategori | Skal behandles automatisk | Behandlingsresultat | Behandlingsstatus | Behandlingssteg      |
      | 1            | 1        | FØRSTEGANGSBEHANDLING | SØKNAD                      | NASJONAL            | UTVIDET       | Nei                       | INNVILGET           | AVSLUTTET         | BEHANDLING_AVSLUTTET |
      | 2            | 1        | REVURDERING           | OPPDATER_UTVIDET_KLASSEKODE | NASJONAL            | UTVIDET       | Ja                        | FORTSATT_INNVILGET  | AVSLUTTET         | BEHANDLING_AVSLUTTET |

  Scenario: skal ikke oppdateres for fagsak som ikke har løpende barnetrygd

    Og med følgende feature toggles
      | BehandlingId | FeatureToggleId                                                | Er togglet på |
      | 1            | familie-ba-sak.skal-bruke-ny-klassekode-for-utvidet-barnetrygd | Nei           |

    Og dagens dato er 15.11.2024
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                       | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD                                | 24.08.1989 |            | OPPFYLT  | Nei                  |

      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER,GIFT_PARTNERSKAP | 13.06.2023 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                                  | 13.06.2023 | 12.06.2041 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.07.2023 | 31.05.2041 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Når vi lager automatisk behandling på fagsak 1 med årsak OPPDATER_UTVIDET_KLASSEKODE

    Så forvent nøyaktig disse behandlingene for fagsak 1
      | BehandlingId | FagsakId | Behandlingstype       | Behandlingsårsak | Behandlingskategori | Underkategori | Skal behandles automatisk | Behandlingsresultat | Behandlingsstatus | Behandlingssteg      |
      | 1            | 1        | FØRSTEGANGSBEHANDLING | SØKNAD           | NASJONAL            | UTVIDET       | Nei                       | INNVILGET           | AVSLUTTET         | BEHANDLING_AVSLUTTET |