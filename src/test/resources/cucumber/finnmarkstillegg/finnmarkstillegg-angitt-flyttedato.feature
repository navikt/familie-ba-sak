# language: no
# encoding: UTF-8

Egenskap: Innvilgelse av Finnmarkstillegg

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | ENDRET_UTBETALING   | SATSENDRING      | Ja                        | NASJONAL            | AVSLUTTET         |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 01.01.2000  |
      | 1            | 2       | BARN       | 01.10.2024  |

  Scenario: Skal prioritere angittFlyttedato over gyldigFraOgMed i bostedsadresse ved oppdatering av utdypende vilkårsvurdering
    Og dagens dato er 13.11.2025
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Fra dato   | Til dato   | Resultat | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               | 01.10.2024 |            | OPPFYLT  | NASJONALE_REGLER |

      | 2       | UNDER_18_ÅR                                 | 01.10.2024 | 30.09.2042 | OPPFYLT  |                  |
      | 2       | BOR_MED_SØKER,LOVLIG_OPPHOLD,BOSATT_I_RIKET | 01.10.2024 |            | OPPFYLT  | NASJONALE_REGLER |
      | 2       | GIFT_PARTNERSKAP                            | 01.10.2024 |            | OPPFYLT  |                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.11.2024 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.05.2025 | 30.09.2042 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |

    Og med adressekommuner
      | AktørId | Angitt flyttedato | Fra dato   | Til dato   | Kommunenummer |
      | 1       | 01.10.2024        | 01.10.2024 | 30.10.2025 | 0301          |
      | 1       | 31.10.2025        | 02.11.2025 |            | 5601          |
      | 2       | 01.10.2024        | 01.10.2024 | 30.10.2025 | 0301          |
      | 2       | 31.10.2025        | 02.11.2025 |            | 5601          |

    Når vi lager automatisk behandling med id 2 på fagsak 1 på grunn av finnmarkstillegg

    Så forvent følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD               |                              | 01.10.2024 |            | OPPFYLT  | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET               |                              | 01.10.2024 | 30.10.2025 | OPPFYLT  | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 31.10.2025 |            | OPPFYLT  | NASJONALE_REGLER |

      | 2       | UNDER_18_ÅR                  |                              | 01.10.2024 | 30.09.2042 | OPPFYLT  |                  |
      | 2       | BOR_MED_SØKER,LOVLIG_OPPHOLD |                              | 01.10.2024 |            | OPPFYLT  | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET               |                              | 01.10.2024 | 30.10.2025 | OPPFYLT  | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 31.10.2025 |            | OPPFYLT  | NASJONALE_REGLER |
      | 2       | GIFT_PARTNERSKAP             |                              | 01.10.2024 |            | OPPFYLT  |                  |
