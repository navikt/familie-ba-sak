# language: no
# encoding: UTF-8

Egenskap: Automatisk valutajustering

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype | Status    |
      | 1        | NORMAL     | OPPRETTET |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus | Behandlingssteg |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | EØS                 | FATTER_VEDTAK     | BESLUTTE_VEDTAK |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 18.01.1986  |              |
      | 1            | 2       | BARN       | 23.01.2008  |              |
      | 1            | 3       | BARN       | 31.03.2015  |              |
      | 1            | 4       | BARN       | 20.10.2017  |              |

    Og følgende dagens dato 10.06.2024
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser                      | Vurderes etter   |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 15.03.2024 |            | OPPFYLT      | Nei                  |                                           | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD   |                              | 01.02.2020 |            | OPPFYLT      | Nei                  |                                           | EØS_FORORDNINGEN |

      | 2       | GIFT_PARTNERSKAP |                              | 23.01.2008 |            | OPPFYLT      | Nei                  |                                           |                  |
      | 2       | UNDER_18_ÅR      |                              | 23.01.2008 | 22.01.2026 | OPPFYLT      | Nei                  |                                           |                  |
      | 2       | LOVLIG_OPPHOLD   |                              | 01.02.2020 |            | OPPFYLT      | Nei                  |                                           | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 01.02.2020 |            | OPPFYLT      | Nei                  |                                           | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER    |                              | 01.02.2020 |            | IKKE_OPPFYLT | Ja                   | AVSLAG_EØS_VURDERING_IKKE_ANSVAR_FOR_BARN | EØS_FORORDNINGEN |

      | 3       | GIFT_PARTNERSKAP |                              | 31.03.2015 |            | OPPFYLT      | Nei                  |                                           |                  |
      | 3       | UNDER_18_ÅR      |                              | 31.03.2015 | 30.03.2033 | OPPFYLT      | Nei                  |                                           |                  |
      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 01.02.2020 |            | OPPFYLT      | Nei                  |                                           | EØS_FORORDNINGEN |
      | 3       | LOVLIG_OPPHOLD   |                              | 01.02.2020 |            | OPPFYLT      | Nei                  |                                           | EØS_FORORDNINGEN |
      | 3       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 01.02.2020 |            | OPPFYLT      | Nei                  |                                           | EØS_FORORDNINGEN |

      | 4       | UNDER_18_ÅR      |                              | 20.10.2017 | 19.10.2035 | OPPFYLT      | Nei                  |                                           |                  |
      | 4       | GIFT_PARTNERSKAP |                              | 20.10.2017 |            | OPPFYLT      | Nei                  |                                           |                  |
      | 4       | LOVLIG_OPPHOLD   |                              | 01.02.2020 |            | OPPFYLT      | Nei                  |                                           | EØS_FORORDNINGEN |
      | 4       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 01.02.2020 |            | OPPFYLT      | Nei                  |                                           | EØS_FORORDNINGEN |
      | 4       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 01.02.2020 |            | OPPFYLT      | Nei                  |                                           | EØS_FORORDNINGEN |

    Og med kompetanser for begrunnelse
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 3, 4    | 01.04.2024 |          | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | LT                             | LT                  |

    Og med utenlandsk periodebeløp for begrunnelse
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 3, 4    | 04.2024   |           | 1            | 150   | EUR         | MÅNEDLIG  | LT              |

    Og med valutakurs for begrunnelse
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs    | Vurderingsform |
      | 3, 4    | 01.04.2024 | 30.04.2024 | 1            | 2024-03-27     | EUR         | 11.6825 | AUTOMATISK     |
      | 3, 4    | 01.05.2024 |            | 1            | 2024-04-30     | EUR         | 11.815  | AUTOMATISK     |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3       | 1            | 01.04.2024 | 30.04.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 1            | 01.05.2024 | 31.05.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 1            | 01.06.2024 | 28.02.2033 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 |

      | 4       | 1            | 01.04.2024 | 30.04.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 4       | 1            | 01.05.2024 | 31.05.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 4       | 1            | 01.06.2024 | 30.09.2035 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 |

    Når vedtaksperiodene genereres for behandling 1

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser | Eøsbegrunnelser                                        | Fritekster |
      | 01.04.2024 | 30.04.2024 |                      | INNVILGET_TILLEGGSTEKST_SATSENDRING_OG_VALUTAJUSTERING |            |


  Scenario: Skal oppdatere valutakursene og andel tilkjente ytelser når vi oppdaterer valutakursene for beslutter
    Når vi oppdaterer valutakursene for beslutter på behandling 1

    Så forvent følgende valutakurser for behandling 1
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs    | Vurderingsform |
      | 3, 4    | 01.04.2024 | 30.04.2024 | 1            | 2024-03-27     | EUR         | 11.6825 | AUTOMATISK     |
      | 3, 4    | 01.05.2024 | 31.05.2024 | 1            | 2024-04-30     | EUR         | 11.815  | AUTOMATISK     |
      | 3, 4    | 01.06.2024 |            | 1            | 2024-05-31     | EUR         | 10      | AUTOMATISK     |

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats | Differanseberegnet beløp |
      | 3       | 1            | 01.04.2024 | 30.04.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 | -242                     |
      | 3       | 1            | 01.05.2024 | 31.05.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 | -262                     |
      | 3       | 1            | 01.06.2024 | 28.02.2033 | 10    | ORDINÆR_BARNETRYGD | 100     | 1510 | 10                       |

      | 4       | 1            | 01.04.2024 | 30.04.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 | -242                     |
      | 4       | 1            | 01.05.2024 | 31.05.2024 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 | -262                     |
      | 4       | 1            | 01.06.2024 | 30.09.2035 | 10    | ORDINÆR_BARNETRYGD | 100     | 1510 | 10                       |

