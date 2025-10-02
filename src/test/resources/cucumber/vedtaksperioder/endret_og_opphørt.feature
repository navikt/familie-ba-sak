# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder ved behandlingsresultat endret og opphørt

  Scenario: Ved endringer i utenlandskperiode beløp i nåværende behandling samt at det er opphør så skal man få endret og opphørt som behandlingsresultat.
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status    |
      | 1        | NORMAL     | AVSLUTTET |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak         | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | FORTSATT_INNVILGET  | MÅNEDLIG_VALUTAJUSTERING | Ja                        | EØS                 | AVSLUTTET         |
      | 2            | 1        | 1                   | ENDRET_OG_OPPHØRT   | NYE_OPPLYSNINGER         | Nei                       | EØS                 | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 15.04.1984  |              |
      | 1            | 2       | BARN       | 30.05.2010  |              |
      | 2            | 1       | SØKER      | 15.04.1984  |              |
      | 2            | 2       | BARN       | 30.05.2010  |              |


    Og dagens dato er 12.05.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 31.07.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD   |                              | 31.07.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | UNDER_18_ÅR      |                              | 30.05.2010 | 29.05.2028 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP |                              | 30.05.2010 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 31.07.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 31.07.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 31.07.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                              | 31.07.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 31.07.2019 | 21.03.2023 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | GIFT_PARTNERSKAP |                              | 30.05.2010 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR      |                              | 30.05.2010 | 29.05.2028 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 31.07.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 31.07.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 31.07.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.08.2019 |            | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |
      | 2       | 01.08.2019 | 31.03.2023 | NORGE_ER_SEKUNDÆRLAND | 2            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |

    Og med utenlandsk periodebeløp
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp   | Valuta kode | Intervall | Utbetalingsland |
      | 2       | 08.2019   |           | 1            | 5023400 | PLN         | MÅNEDLIG  | PL              |
      | 2       | 08.2019   | 03.2023   | 2            | 500     | PLN         | MÅNEDLIG  | PL              |

    Og med valutakurser
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2       | 01.08.2019 | 30.09.2024 | 1            | 2019-12-31     | PLN         | 2.3171866191 | MANUELL        |
      | 2       | 01.10.2024 | 31.10.2024 | 1            | 2024-09-30     | PLN         | 2.7494858372 | AUTOMATISK     |
      | 2       | 01.11.2024 | 30.11.2024 | 1            | 2024-10-31     | PLN         | 2.7439781190 | AUTOMATISK     |
      | 2       | 01.12.2024 | 31.12.2024 | 1            | 2024-11-29     | PLN         | 2.7189245810 | AUTOMATISK     |
      | 2       | 01.01.2025 | 31.01.2025 | 1            | 2024-12-31     | PLN         | 2.7590643275 | AUTOMATISK     |
      | 2       | 01.02.2025 | 28.02.2025 | 1            | 2025-01-31     | PLN         | 2.7859719915 | AUTOMATISK     |
      | 2       | 01.03.2025 | 31.03.2025 | 1            | 2025-02-28     | PLN         | 2.8249765077 | AUTOMATISK     |
      | 2       | 01.04.2025 |            | 1            | 2025-03-31     | PLN         | 2.7277724665 | AUTOMATISK     |
      | 2       | 01.08.2019 | 31.12.2019 | 2            | 2019-12-31     | PLN         | 2.3171866191 | MANUELL        |
      | 2       | 01.01.2020 | 31.12.2020 | 2            | 2020-12-31     | PLN         | 2.2962694914 | MANUELL        |
      | 2       | 01.01.2021 | 31.12.2021 | 2            | 2021-12-31     | PLN         | 2.1729426353 | MANUELL        |
      | 2       | 01.01.2022 | 31.12.2022 | 2            | 2022-12-30     | PLN         | 2.2461545035 | MANUELL        |
      | 2       | 01.01.2023 | 31.03.2023 | 2            | 2023-12-29     | PLN         | 2.5902753773 | MANUELL        |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.08.2019 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 31.12.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 1            | 01.01.2024 | 31.08.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 1            | 01.09.2024 | 30.09.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.10.2024 | 31.10.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.11.2024 | 30.11.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.12.2024 | 31.12.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.01.2025 | 31.01.2025 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.02.2025 | 28.02.2025 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.03.2025 | 31.03.2025 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.04.2025 | 30.04.2028 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |

      | 2       | 2            | 01.08.2019 | 31.12.2019 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.01.2020 | 31.12.2020 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.01.2021 | 31.12.2021 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.01.2022 | 31.12.2022 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.01.2023 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.03.2023 | 31.03.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1083 |

    Når vedtaksperiodene genereres for behandling 2

    Og når behandlingsresultatet er utledet for behandling 2
    Så forvent at behandlingsresultatet er ENDRET_OG_OPPHØRT på behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                                       | Ugyldige begrunnelser |
      | 01.08.2019 | 31.03.2023 | UTBETALING         | EØS_FORORDNINGEN               | INNVILGET_SEKUNDÆRLAND_STANDARD, INNVILGET_TILLEGGSTEKST_UTBETALINGSTABELL |                       |
      | 01.04.2023 |            | OPPHØR             | EØS_FORORDNINGEN               | OPPHØR_EØS_STANDARD                                                        |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser | Eøsbegrunnelser                                                            | Fritekster |
      | 01.08.2019 | 31.03.2023 |                      | INNVILGET_SEKUNDÆRLAND_STANDARD, INNVILGET_TILLEGGSTEKST_UTBETALINGSTABELL |            |
      | 01.04.2023 |            |                      | OPPHØR_EØS_STANDARD                                                        |            |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.08.2019 til 31.03.2023
      | Begrunnelse                               | Type | Barnas fødselsdatoer | Antall barn | Målform | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland | Gjelder søker |
      | INNVILGET_SEKUNDÆRLAND_STANDARD           | EØS  | 30.05.10             | 1           | NB      | Arbeider         | I_ARBEID                  | Norge                 | Polen                          | Polen               | Nei           |
      | INNVILGET_TILLEGGSTEKST_UTBETALINGSTABELL | EØS  | 30.05.10             | 1           | NB      | Arbeider         | I_ARBEID                  | Norge                 | Polen                          | Polen               | Nei           |
