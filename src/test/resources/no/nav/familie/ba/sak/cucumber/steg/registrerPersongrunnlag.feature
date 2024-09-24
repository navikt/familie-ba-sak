# language: no
# encoding: UTF-8

Egenskap: Registrer persongrunnlag steg

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak     | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus | Behandlingstype         |
      | 1            | 1        |                     | INNVILGET           | HELMANUELL_MIGRERING | Nei                       | EØS                 | AVSLUTTET         | MIGRERING_FRA_INFOTRYGD |
      | 2            | 1        | 1                   | FORTSATT_INNVILGET  | TEKNISK_ENDRING      | Nei                       | EØS                 | AVSLUTTET         | REVURDERING             |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1, 2         | 1       | SØKER      | 03.07.1984  |              |
      | 1, 2         | 2       | BARN       | 01.01.2009  |              |
      | 2            | 3       | BARN       | 01.01.2007  |              |

  Scenario: Skal ikke inkludere barn som ble opphørt forrige behandling
    Og dagens dato er 11.06.2024
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 31.07.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD   |                              | 31.07.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | GIFT_PARTNERSKAP |                              | 01.01.2009 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR      |                              | 01.01.2009 | 31.12.2026 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 31.07.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_NORGE_MED_SØKER   | 31.07.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 31.07.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                              | 31.07.2019 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 31.07.2019 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN |

      | 3       | UNDER_18_ÅR      |                              | 01.01.2007 | 31.12.2024 | OPPFYLT      | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP |                              | 01.01.2007 |            | OPPFYLT      | Nei                  |                      |                  |
      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 31.07.2019 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | LOVLIG_OPPHOLD   |                              | 31.07.2019 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 31.07.2019 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | UNDER_18_ÅR      |                              | 01.01.2009 | 31.12.2026 | OPPFYLT      | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP |                              | 01.01.2009 |            | OPPFYLT      | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER    |                              | 31.07.2019 |            | IKKE_OPPFYLT | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 31.07.2019 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 31.07.2019 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.08.2019 |          | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |
      | 3       | 01.08.2019 |          | NORGE_ER_SEKUNDÆRLAND | 2            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |

    Og med utenlandsk periodebeløp
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2       | 08.2019   |           | 1            | 5000  | PLN         | MÅNEDLIG  | PL              |
      | 3       | 08.2019   |           | 2            | 5000  | PLN         | MÅNEDLIG  | PL              |

    Og med valutakurser
      | AktørId | Fra dato   | Til dato | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2       | 01.08.2019 |          | 1            | 2019-07-31     | PLN         | 2.2785700969 | MANUELL        |
      | 3       | 01.08.2019 |          | 2            | 2019-07-31     | PLN         | 2.2785700969 | MANUELL        |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.08.2019 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 31.12.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 1            | 01.01.2024 | 31.12.2026 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 |

      | 3       | 2            | 01.08.2019 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 2            | 01.07.2023 | 31.12.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 2            | 01.01.2024 | 31.08.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 2            | 01.09.2024 | 31.12.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Når vi lager automatisk behandling med id 3 på fagsak 1 på grunn av automatisk valutajustering og har følgende valutakurser
      | Valuta kode | Valutakursdato | Kurs         |
      | PLN         | 31.05.2024     | 2.6692461015 |

    Så forvent følgende aktører på behandling 3
      | AktørId |
      | 1       |
      | 3       |