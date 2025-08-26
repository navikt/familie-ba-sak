# language: no
# encoding: UTF-8

Egenskap: Automatisk valutajustering ved opphør

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | ÅRLIG_KONTROLL   | Nei                       | EØS                 | AVSLUTTET         |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 2            | 1       | SØKER      | 11.01.1978  |              |
      | 2            | 2       | BARN       | 22.04.2006  |              |
      | 2            | 3       | BARN       | 04.04.2007  |              |

  Scenario: Når barnetrygden opphører for barn mellom behandlinger skal ikke opphørt barn dras med i månedlig valutajustering
    Og dagens dato er 03.06.2024
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                              | 31.12.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 31.05.2009 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | UNDER_18_ÅR      |                              | 22.04.2006 | 21.04.2024 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP |                              | 22.04.2006 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 31.05.2009 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 31.05.2009 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 31.05.2009 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 3       | GIFT_PARTNERSKAP |                              | 04.04.2007 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR      |                              | 04.04.2007 | 03.04.2025 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 31.05.2009 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 31.05.2009 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | LOVLIG_OPPHOLD   |                              | 31.05.2009 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 2            | 01.01.2022 | 31.12.2022 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.01.2023 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.07.2023 | 31.12.2023 | 15    | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 2            | 01.01.2024 | 31.03.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 2            | 01.01.2022 | 31.12.2022 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 2            | 01.01.2023 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 2            | 01.07.2023 | 31.12.2023 | 15    | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 2            | 01.01.2024 | 31.08.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 2            | 01.09.2024 | 31.03.2025 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2, 3    | 01.01.2022 |          | NORGE_ER_SEKUNDÆRLAND | 2            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |

    Og med utenlandsk periodebeløp
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2, 3    | 01.2022   | 12.2023   | 2            | 500   | PLN         | MÅNEDLIG  | PL              |
      | 2, 3    | 01.2024   |           | 2            | 800   | PLN         | MÅNEDLIG  | PL              |

    Og med valutakurser
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         |
      | 2, 3    | 01.01.2022 | 31.12.2022 | 2            | 01.11.2019     | PLN         | 2.2461545035 |
      | 2, 3    | 01.01.2023 |            | 2            | 01.11.2019     | PLN         | 2.5902753773 |

    Når vi lager automatisk behandling med id 3 på fagsak 1 på grunn av automatisk valutajustering og har følgende valutakurser
      | Valuta kode | Valutakursdato | Kurs |
      | PLN         | 31.05.2024     | 3    |

    Så forvent følgende valutakurser for behandling 3
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2, 3    | 01.01.2022 | 31.12.2022 | 3            | 01.11.2019     | PLN         | 2.2461545035 | MANUELL        |
      | 2, 3    | 01.01.2023 | 31.03.2024 | 3            | 01.11.2019     | PLN         | 2.5902753773 | MANUELL        |
      | 3       | 01.04.2024 | 31.05.2024 | 3            | 01.11.2019     | PLN         | 2.5902753773 | MANUELL        |
      | 3       | 01.06.2024 |            | 3            | 31.05.2024     | PLN         | 3            | AUTOMATISK     |