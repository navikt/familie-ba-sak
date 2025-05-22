# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser ved opphør for EØS.

  Scenario: Skal ikke dra inn barn som har blitt avslått tidligere i behandlingen i opphørsbegrunnelse i EØS-sak
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | EØS                 |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 06.06.1994  |              |
      | 1            | 2       | BARN       | 16.01.2020  |              |
      | 1            | 3       | BARN       | 25.06.2020  |              |

    Og dagens dato er 23.02.2022
    Og lag personresultater for behandling 1

    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 2       |
      | 1            | 3       |

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser            | Vurderes etter   |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 01.09.2019 | 31.07.2020 | OPPFYLT      | Nei                  |                                 | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD   |                              | 01.09.2019 | 31.07.2020 | OPPFYLT      | Nei                  |                                 | EØS_FORORDNINGEN |

      | 2       | UNDER_18_ÅR      |                              | 16.01.2020 | 15.01.2038 | OPPFYLT      | Nei                  |                                 |                  |
      | 2       | BOR_MED_SØKER    |                              | 16.01.2020 |            | IKKE_OPPFYLT | Ja                   | AVSLAG_EØS_IKKE_ANSVAR_FOR_BARN | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 16.01.2020 |            | OPPFYLT      | Nei                  |                                 | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 16.01.2020 |            | OPPFYLT      | Nei                  |                                 | EØS_FORORDNINGEN |
      | 2       | GIFT_PARTNERSKAP |                              | 16.01.2020 |            | OPPFYLT      | Nei                  |                                 |                  |

      | 3       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 25.06.2020 |            | OPPFYLT      | Nei                  |                                 | EØS_FORORDNINGEN |
      | 3       | UNDER_18_ÅR      |                              | 25.06.2020 | 24.06.2038 | OPPFYLT      | Nei                  |                                 |                  |
      | 3       | LOVLIG_OPPHOLD   |                              | 25.06.2020 |            | OPPFYLT      | Nei                  |                                 | EØS_FORORDNINGEN |
      | 3       | GIFT_PARTNERSKAP |                              | 25.06.2020 |            | OPPFYLT      | Nei                  |                                 |                  |
      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 25.06.2020 |            | OPPFYLT      | Nei                  |                                 | EØS_FORORDNINGEN |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3       | 1            | 01.07.2020 | 31.07.2020 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 3       | 01.07.2020 | 31.07.2020 | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |

    Når vedtaksperiodene genereres for behandling 1

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato | Standardbegrunnelser | Eøsbegrunnelser                 | Fritekster |
      | 01.02.2020 |          |                      | AVSLAG_EØS_IKKE_ANSVAR_FOR_BARN |            |
      | 01.08.2020 |          |                      | OPPHØR_EØS_STANDARD             |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.08.2020 til -
      | Begrunnelse         | Type | Barnas fødselsdatoer | Antall barn | Gjelder søker | Målform |
      | OPPHØR_EØS_STANDARD | EØS  | 25.06.20             | 1           | Ja            | NB      |

  Scenario: Barn med nullutbetaling i forrige periode skal kunne begrunnes dersom det bare eksisterer barn med nullutbetaling
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak         | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | FORTSATT_INNVILGET  | MÅNEDLIG_VALUTAJUSTERING | Ja                        | EØS                 | AVSLUTTET         |
      | 2            | 1        | 1                   | OPPHØRT             | NYE_OPPLYSNINGER         | Nei                       | EØS                 | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 18.11.1991  |              |
      | 1            | 2       | BARN       | 24.03.2022  |              |
      | 2            | 1       | SØKER      | 18.11.1991  |              |
      | 2            | 2       | BARN       | 24.03.2022  |              |
    Og dagens dato er 22.05.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår                    | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING        | 01.03.2016 | 31.05.2022 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD   |                                     | 24.03.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING_UTLAND | 01.06.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER            | 24.03.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | GIFT_PARTNERSKAP |                                     | 24.03.2022 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR      |                                     | 24.03.2022 | 23.03.2040 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS                      | 24.03.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                                     | 24.03.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår                    | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING        | 01.03.2016 | 31.05.2022 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD   |                                     | 24.03.2022 | 30.01.2024 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING_UTLAND | 01.06.2022 | 30.01.2024 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | UNDER_18_ÅR      |                                     | 24.03.2022 | 23.03.2040 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS                      | 24.03.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | GIFT_PARTNERSKAP |                                     | 24.03.2022 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER            | 24.03.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                                     | 24.03.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet                     | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.04.2022 | 31.05.2022 | NORGE_ER_PRIMÆRLAND   | 1            | ARBEIDER                             | INAKTIV                   | NO                    | PL                             | PL                  |
      | 2       | 01.06.2022 |            | NORGE_ER_SEKUNDÆRLAND | 1            | MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN | I_ARBEID                  | NO                    | PL                             | PL                  |
      | 2       | 01.04.2022 | 31.05.2022 | NORGE_ER_PRIMÆRLAND   | 2            | ARBEIDER                             | INAKTIV                   | NO                    | PL                             | PL                  |
      | 2       | 01.06.2022 | 31.01.2024 | NORGE_ER_SEKUNDÆRLAND | 2            | MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN | I_ARBEID                  | NO                    | PL                             | PL                  |

    Og med utenlandsk periodebeløp
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2       | 06.2022   | 12.2023   | 1            | 500   | PLN         | MÅNEDLIG  | PL              |
      | 2       | 01.2024   |           | 1            | 800   | PLN         | MÅNEDLIG  | PL              |
      | 2       | 06.2022   | 12.2023   | 2            | 500   | PLN         | MÅNEDLIG  | PL              |
      | 2       | 01.2024   | 01.2024   | 2            | 800   | PLN         | MÅNEDLIG  | PL              |

    Og med valutakurser
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2       | 01.06.2022 | 31.12.2022 | 1            | 2022-12-30     | PLN         | 2.2461545035 | MANUELL        |
      | 2       | 01.01.2023 | 31.05.2024 | 1            | 2023-12-29     | PLN         | 2.5902753773 | MANUELL        |
      | 2       | 01.06.2024 | 30.06.2024 | 1            | 2024-05-31     | PLN         | 2.6692461015 | AUTOMATISK     |
      | 2       | 01.07.2024 | 31.07.2024 | 1            | 2024-06-28     | PLN         | 2.6448131817 | AUTOMATISK     |
      | 2       | 01.08.2024 | 31.08.2024 | 1            | 2024-07-31     | PLN         | 2.7541484106 | AUTOMATISK     |
      | 2       | 01.09.2024 | 30.09.2024 | 1            | 2024-08-30     | PLN         | 2.7271239155 | AUTOMATISK     |
      | 2       | 01.10.2024 | 31.10.2024 | 1            | 2024-09-30     | PLN         | 2.7494858372 | AUTOMATISK     |
      | 2       | 01.11.2024 | 30.11.2024 | 1            | 2024-10-31     | PLN         | 2.7439781190 | AUTOMATISK     |
      | 2       | 01.12.2024 | 31.12.2024 | 1            | 2024-11-29     | PLN         | 2.7189245810 | AUTOMATISK     |
      | 2       | 01.01.2025 | 31.01.2025 | 1            | 2024-12-31     | PLN         | 2.7590643275 | AUTOMATISK     |
      | 2       | 01.02.2025 | 28.02.2025 | 1            | 2025-01-31     | PLN         | 2.7859719915 | AUTOMATISK     |
      | 2       | 01.03.2025 | 31.03.2025 | 1            | 2025-02-28     | PLN         | 2.8249765077 | AUTOMATISK     |
      | 2       | 01.04.2025 | 30.04.2025 | 1            | 2025-03-31     | PLN         | 2.7277724665 | AUTOMATISK     |
      | 2       | 01.05.2025 |            | 1            | 2025-04-30     | PLN         | 2.7621453465 | AUTOMATISK     |
      | 2       | 01.06.2022 | 31.12.2022 | 2            | 2022-12-30     | PLN         | 2.2461545035 | MANUELL        |
      | 2       | 01.01.2023 | 31.01.2024 | 2            | 2023-12-29     | PLN         | 2.5902753773 | MANUELL        |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.04.2022 | 31.05.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 1            | 01.06.2022 | 31.12.2022 | 553   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 1            | 01.01.2023 | 28.02.2023 | 381   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 428   | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 2       | 1            | 01.07.2023 | 31.12.2023 | 471   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.01.2024 | 31.05.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.06.2024 | 30.06.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.07.2024 | 31.07.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.08.2024 | 31.08.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.09.2024 | 30.09.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.10.2024 | 31.10.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.11.2024 | 30.11.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.12.2024 | 31.12.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.01.2025 | 31.01.2025 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.02.2025 | 28.02.2025 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.03.2025 | 31.03.2025 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.04.2025 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.05.2025 | 29.02.2040 | 0     | ORDINÆR_BARNETRYGD | 100     | 1968 |

      | 2       | 2            | 01.04.2022 | 31.05.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 2            | 01.06.2022 | 31.12.2022 | 553   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 2            | 01.01.2023 | 28.02.2023 | 381   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 428   | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 2       | 2            | 01.07.2023 | 31.12.2023 | 471   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 2            | 01.01.2024 | 31.01.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Når vedtaksperiodene genereres for behandling 2


    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser | Ugyldige begrunnelser |
      | 01.02.2024 |          | OPPHØR             |                                |                      |                       |
      | 01.02.2024 |          | OPPHØR             | EØS_FORORDNINGEN               | OPPHØR_EØS_STANDARD  |                       |


    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser | Eøsbegrunnelser     | Fritekster |
      | 01.02.2024 |          |                      | OPPHØR_EØS_STANDARD |            |


    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.02.2024 til -
      | Begrunnelse         | Type | Barnas fødselsdatoer | Antall barn | Målform | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland | Gjelder søker |
      | OPPHØR_EØS_STANDARD | EØS  | 24.03.22             | 1           |         |                  |                           |                       |                                |                     | Ja            |