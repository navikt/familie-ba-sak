# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser selvstendig rett opphør fra start

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | Nei                       | EØS                 |
      | 2            | 1        | 1                   | OPPHØRT             | NYE_OPPLYSNINGER | Nei                       | EØS                 |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 26.02.1989  |
      | 1            | 2       | BARN       | 13.11.2007  |
      | 2            | 1       | SØKER      | 26.02.1989  |
      | 2            | 2       | BARN       | 13.11.2007  |

  Scenario: Skal flette inn barnas kompetanser ved opphør selvstendig rett når søker mister vilkår
    Og dagens dato er 13.12.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår                            | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                                             | 01.09.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING | 01.09.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | UNDER_18_ÅR      |                                             | 13.11.2007 | 12.11.2025 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP |                                             | 13.11.2007 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS                              | 01.09.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                                             | 01.09.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER                    | 01.09.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår         | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET   |                          | 01.09.2023 |            | IKKE_OPPFYLT | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD   |                          | 01.09.2023 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | GIFT_PARTNERSKAP |                          | 13.11.2007 |            | OPPFYLT      | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR      |                          | 13.11.2007 | 12.11.2025 | OPPFYLT      | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER | 01.09.2023 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                          | 01.09.2023 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS           | 01.09.2023 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.10.2023 | 31.10.2025 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |


    Og med kompetanser
      | AktørId | Fra dato   | Til dato | Resultat            | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.10.2023 |          | NORGE_ER_PRIMÆRLAND | 1            | INAKTIV          | ARBEIDER                  | BE                    | NO                             | BE                  |

    Når vedtaksperiodene genereres for behandling 2
    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                     | Ugyldige begrunnelser |
      | 01.10.2023 |          | OPPHØR             | EØS_FORORDNINGEN               | OPPHOR_SELVSTENDIG_RETT_OPPHOR_FRA_START |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser | Eøsbegrunnelser                          | Fritekster |
      | 01.10.2023 |          |                      | OPPHOR_SELVSTENDIG_RETT_OPPHOR_FRA_START |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.10.2023 til -
      | Begrunnelse                              | Type | Barnas fødselsdatoer | Antall barn | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | OPPHOR_SELVSTENDIG_RETT_OPPHOR_FRA_START | EØS  | 13.11.07             | 1           | INAKTIV          | ARBEIDER                  | Belgia                | Norge                          | Belgia              |