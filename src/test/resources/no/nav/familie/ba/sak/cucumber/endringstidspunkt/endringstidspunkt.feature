# language: no
# encoding: UTF-8

Egenskap: Endringstidspunkt

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | ENDRET_UTBETALING   | ÅRLIG_KONTROLL   | Nei                       | EØS                 | AVSLUTTET         |
      | 2            | 1        | 1                   | ENDRET_OG_OPPHØRT   | NYE_OPPLYSNINGER | Nei                       | EØS                 | UTREDES           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 06.07.1992  |              |
      | 1            | 2       | BARN       | 06.08.2020  |              |
      | 2            | 1       | SØKER      | 06.07.1992  |              |
      | 2            | 2       | BARN       | 06.08.2020  |              |

  Scenario: Skal sette opphørsdato som første endringstidspunkt
    Og følgende dagens dato 06.06.2024
    Og lag personresultater for begrunnelse for behandling 1
    Og lag personresultater for begrunnelse for behandling 2

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                              | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 01.02.2022 | 31.01.2023 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 08.05.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | UNDER_18_ÅR      |                              | 06.08.2020 | 05.08.2038 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP |                              | 06.08.2020 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | LOVLIG_OPPHOLD   |                              | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og legg til nye vilkårresultater for begrunnelse for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 01.02.2022 | 31.01.2023 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD   |                              | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 08.05.2023 | 31.05.2024 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | UNDER_18_ÅR      |                              | 06.08.2020 | 05.08.2038 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP |                              | 06.08.2020 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 01.02.2022 | 31.05.2024 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og med kompetanser for begrunnelse
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.03.2022 | 30.04.2022 | NORGE_ER_PRIMÆRLAND   | 1            | ARBEIDER         | INAKTIV                   | NO                    | PL                             | PL                  |
      | 2       | 01.05.2022 | 31.01.2023 | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |
      | 2       | 01.06.2023 |            | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |
      | 2       | 01.03.2022 | 30.04.2022 | NORGE_ER_PRIMÆRLAND   | 2            | ARBEIDER         | INAKTIV                   | NO                    | PL                             | PL                  |
      | 2       | 01.05.2022 | 31.01.2023 | NORGE_ER_SEKUNDÆRLAND | 2            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |
      | 2       | 01.06.2023 | 31.05.2024 | NORGE_ER_SEKUNDÆRLAND | 2            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |

    Og med utenlandsk periodebeløp for begrunnelse
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2       | 06.2023   | 12.2023   | 1            | 500   | PLN         | MÅNEDLIG  | PL              |
      | 2       | 01.2024   |           | 1            | 800   | PLN         | MÅNEDLIG  | PL              |
      | 2       | 05.2022   | 01.2023   | 1            | 500   | PLN         | MÅNEDLIG  | PL              |
      | 2       | 06.2023   | 12.2023   | 2            | 500   | PLN         | MÅNEDLIG  | PL              |
      | 2       | 05.2022   | 01.2023   | 2            | 500   | PLN         | MÅNEDLIG  | PL              |
      | 2       | 01.2024   | 05.2024   | 2            | 800   | PLN         | MÅNEDLIG  | PL              |

    Og med valutakurs for begrunnelse
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         |
      | 2       | 01.05.2022 | 31.12.2022 | 1            | 2022-12-30     | PLN         | 2.2461545035 |
      | 2       | 01.01.2023 | 31.01.2023 | 1            | 2023-12-29     | PLN         | 2.5902753773 |
      | 2       | 01.06.2023 |            | 1            | 2023-12-29     | PLN         | 2.5902753773 |
      | 2       | 01.05.2022 | 31.12.2022 | 2            | 2022-12-30     | PLN         | 2.2461545035 |
      | 2       | 01.01.2023 | 31.01.2023 | 2            | 2023-12-29     | PLN         | 2.5902753773 |
      | 2       | 01.06.2023 |            | 2            | 2023-12-29     | PLN         | 2.5902753773 |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.03.2022 | 30.04.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 1            | 01.05.2022 | 31.12.2022 | 553   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 1            | 01.01.2023 | 31.01.2023 | 381   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 1            | 01.06.2023 | 30.06.2023 | 428   | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 2       | 1            | 01.07.2023 | 31.12.2023 | 471   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.01.2024 | 31.07.2026 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.08.2026 | 31.07.2038 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 |

      | 2       | 2            | 01.03.2022 | 30.04.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 2            | 01.05.2022 | 31.12.2022 | 553   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 2            | 01.01.2023 | 31.01.2023 | 381   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 2            | 01.06.2023 | 30.06.2023 | 428   | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 2       | 2            | 01.07.2023 | 31.12.2023 | 471   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 2            | 01.01.2024 | 31.05.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Så forvent at endringstidspunktet er 01.06.2024 for behandling 2