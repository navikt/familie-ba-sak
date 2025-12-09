# language: no
# encoding: UTF-8

Egenskap: Månedlig valutajustering når barn fyller 18 år

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak         | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | FORTSATT_INNVILGET  | MÅNEDLIG_VALUTAJUSTERING | Ja                        | EØS                 | AVSLUTTET         |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 01.01.1980  |
      | 1            | 2       | BARN       | 01.02.2007  |
      | 1            | 3       | BARN       | 01.06.2010  |

    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Vurderes etter   |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 31.12.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD   |                              | 31.12.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

      | 2       | GIFT_PARTNERSKAP |                              | 01.02.2007 |            | OPPFYLT  | Nei                  |                  |
      | 2       | UNDER_18_ÅR      |                              | 01.02.2007 | 31.01.2025 | OPPFYLT  | Nei                  |                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 01.08.2024 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 01.08.2024 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 01.08.2024 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

      | 3       | UNDER_18_ÅR      |                              | 01.06.2010 | 31.05.2028 | OPPFYLT  | Nei                  |                  |
      | 3       | GIFT_PARTNERSKAP |                              | 01.06.2010 |            | OPPFYLT  | Nei                  |                  |
      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 01.08.2024 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 3       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 01.08.2024 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 3       | LOVLIG_OPPHOLD   |                              | 01.08.2024 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2, 3    | 01.08.2024 |          | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | NO                  |

    Og med utenlandsk periodebeløp
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2, 3    | 09.2024   |           | 1            | 800   | PLN         | MÅNEDLIG  | PL              |

    Og med valutakurser
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2, 3    | 01.09.2024 | 30.09.2024 | 1            | 2024-08-30     | PLN         | 2.7271239155 | AUTOMATISK     |
      | 2, 3    | 01.10.2024 | 31.10.2024 | 1            | 2024-09-30     | PLN         | 2.7494858372 | AUTOMATISK     |
      | 2, 3    | 01.11.2024 | 30.11.2024 | 1            | 2024-10-31     | PLN         | 2.7439781190 | AUTOMATISK     |
      | 2, 3    | 01.12.2024 |            | 1            | 2024-11-29     | PLN         | 2.7189245810 | AUTOMATISK     |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.09.2024 | 31.01.2025 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.09.2024 | 31.05.2028 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |

  Scenario: Skal oppdatere valutakurser som har fra og med-dato etter inneværende måned
    Og dagens dato er 01.01.2025
    Og kopier persongrunnlag fra behandling 1 til behandling 2

    Når vi lager automatisk behandling med id 2 på fagsak 1 på grunn av automatisk valutajustering og har følgende valutakurser
      | Valuta kode | Valutakursdato | Kurs         |
      | PLN         | 31.12.2024     | 2.7590643275 |

    Så forvent følgende valutakurser for behandling 2
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2, 3    | 01.09.2024 | 30.09.2024 | 2            | 2024-08-30     | PLN         | 2.7271239155 | AUTOMATISK     |
      | 2, 3    | 01.10.2024 | 31.10.2024 | 2            | 2024-09-30     | PLN         | 2.7494858372 | AUTOMATISK     |
      | 2, 3    | 01.11.2024 | 30.11.2024 | 2            | 2024-10-31     | PLN         | 2.7439781190 | AUTOMATISK     |
      | 2, 3    | 01.12.2024 | 31.12.2024 | 2            | 2024-11-29     | PLN         | 2.7189245810 | AUTOMATISK     |
      | 2, 3    | 01.01.2025 | 31.01.2025 | 2            | 2024-12-31     | PLN         | 2.7590643275 | AUTOMATISK     |
      | 3       | 01.02.2025 |            | 2            | 2024-12-31     | PLN         | 2.7590643275 | AUTOMATISK     |

  Scenario: Skal oppdatere valutakurs inneværende måned hvis det eksisterer en valutakurs for inneværende måned med feil valutakursdato
    Og dagens dato er 01.01.2025
    Og kopier persongrunnlag fra behandling 1 til behandling 2

    Når vi lager automatisk behandling med id 2 på fagsak 1 på grunn av automatisk valutajustering og har følgende valutakurser
      | Valuta kode | Valutakursdato | Kurs         |
      | PLN         | 31.12.2024     | 2.7590643275 |

    Og dagens dato er 01.02.2025
    Og kopier persongrunnlag fra behandling 1 til behandling 3

    Når vi lager automatisk behandling med id 3 på fagsak 1 på grunn av automatisk valutajustering og har følgende valutakurser
      | Valuta kode | Valutakursdato | Kurs         |
      | PLN         | 31.01.2025     | 2.8249765077 |

    Så forvent følgende valutakurser for behandling 3
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2, 3    | 01.09.2024 | 30.09.2024 | 3            | 2024-08-30     | PLN         | 2.7271239155 | AUTOMATISK     |
      | 2, 3    | 01.10.2024 | 31.10.2024 | 3            | 2024-09-30     | PLN         | 2.7494858372 | AUTOMATISK     |
      | 2, 3    | 01.11.2024 | 30.11.2024 | 3            | 2024-10-31     | PLN         | 2.7439781190 | AUTOMATISK     |
      | 2, 3    | 01.12.2024 | 31.12.2024 | 3            | 2024-11-29     | PLN         | 2.7189245810 | AUTOMATISK     |
      | 2, 3    | 01.01.2025 | 31.01.2025 | 3            | 2024-12-31     | PLN         | 2.7590643275 | AUTOMATISK     |
      | 3       | 01.02.2025 |            | 3            | 2025-01-31     | PLN         | 2.8249765077 | AUTOMATISK     |
