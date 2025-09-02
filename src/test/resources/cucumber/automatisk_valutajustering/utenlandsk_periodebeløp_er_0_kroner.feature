# language: no
# encoding: UTF-8

Egenskap: Automatiske valutakurser - Utenlandsk periodebeløp er 0 kroner

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat         | Behandlingsårsak         | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | FORTSATT_INNVILGET          | MÅNEDLIG_VALUTAJUSTERING | Ja                        | EØS                 | AVSLUTTET         |
      | 2            | 1        | 1                   | HENLAGT_FEILAKTIG_OPPRETTET | ÅRLIG_KONTROLL           | Nei                       | EØS                 | AVSLUTTET         |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 24.11.1987  |              |
      | 1            | 2       | BARN       | 12.05.2015  |              |
      | 1            | 3       | BARN       | 08.02.2019  |              |
      | 2            | 1       | SØKER      | 24.11.1987  |              |
      | 2            | 2       | BARN       | 12.05.2015  |              |
      | 2            | 3       | BARN       | 08.02.2019  |              |

  Scenario: Automatisk oppdatering av valutakurser skal skje selv om det ikke fører til noen endringer i andelene
    Og dagens dato er 24.06.2024
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                              | 01.11.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 01.11.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | UNDER_18_ÅR      |                              | 12.05.2015 | 11.05.2033 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP |                              | 12.05.2015 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 01.11.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 01.11.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 01.11.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 3       | GIFT_PARTNERSKAP |                              | 08.02.2019 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR      |                              | 08.02.2019 | 07.02.2037 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 01.11.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 01.11.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | LOVLIG_OPPHOLD   |                              | 01.11.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 01.11.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD   |                              | 01.11.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | UNDER_18_ÅR      |                              | 12.05.2015 | 11.05.2033 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP |                              | 12.05.2015 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | LOVLIG_OPPHOLD   |                              | 01.11.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 01.11.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 01.11.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 3       | UNDER_18_ÅR      |                              | 08.02.2019 | 07.02.2037 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP |                              | 08.02.2019 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 01.11.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | LOVLIG_OPPHOLD   |                              | 01.11.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 01.11.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2, 3    | 01.12.2023 |          | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | BG                             | BG                  |
      | 2, 3    | 01.12.2023 |          | NORGE_ER_SEKUNDÆRLAND | 2            | ARBEIDER         | I_ARBEID                  | NO                    | BG                             | BG                  |

    Og med utenlandsk periodebeløp
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2, 3    | 12.2023   |           | 1            | 0     | BGN         | MÅNEDLIG  | BG              |
      | 2, 3    | 12.2023   |           | 2            | 0     | BGN         | MÅNEDLIG  | BG              |

    Og med valutakurser
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2, 3    | 01.12.2023 | 31.05.2024 | 1            | 2023-07-31     | BGN         | 5.7165865630 | MANUELL        |
      | 2, 3    | 01.06.2024 |            | 1            | 2024-05-31     | BGN         | 5.8201247571 | AUTOMATISK     |
      | 2, 3    | 01.12.2023 | 31.12.2023 | 2            | 2023-07-31     | BGN         | 5.7165865630 | MANUELL        |
      | 2, 3    | 01.01.2024 |            | 2            |                | BGN         |              | IKKE_VURDERT   |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.12.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 1            | 01.01.2024 | 30.04.2033 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 1            | 01.12.2023 | 31.01.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.02.2025 | 31.01.2037 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |

      | 2       | 2            | 01.12.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 2            | 01.01.2024 | 30.04.2033 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 2            | 01.12.2023 | 31.12.2023 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 2            | 01.01.2024 | 31.01.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 2            | 01.02.2025 | 31.01.2037 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |

    Når vi automatisk oppdaterer valutakurser for behandling 2

    Så forvent følgende valutakurser for behandling 2
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2, 3    | 01.12.2023 | 31.12.2023 | 2            | 2023-07-31     | BGN         | 5.7165865630 | MANUELL        |
      | 2, 3    | 01.01.2024 | 31.01.2024 | 2            | 2023-12-29     | BGN         | 10           | AUTOMATISK     |
      | 2, 3    | 01.02.2024 | 29.02.2024 | 2            | 2024-01-31     | BGN         | 10           | AUTOMATISK     |
      | 2, 3    | 01.03.2024 | 31.03.2024 | 2            | 2024-02-29     | BGN         | 10           | AUTOMATISK     |
      | 2, 3    | 01.04.2024 | 30.04.2024 | 2            | 2024-03-27     | BGN         | 10           | AUTOMATISK     |
      | 2, 3    | 01.05.2024 | 31.05.2024 | 2            | 2024-04-30     | BGN         | 10           | AUTOMATISK     |
      | 2, 3    | 01.06.2024 |            | 2            | 2024-05-31     | BGN         | 5.8201247571 | AUTOMATISK     |
