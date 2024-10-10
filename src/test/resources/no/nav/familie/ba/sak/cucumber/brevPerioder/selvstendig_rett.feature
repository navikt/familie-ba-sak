# language: no
# encoding: UTF-8

Egenskap: Brevperioder: Selvstendig rett

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | EØS                 |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 21.06.1983  |
      | 1            | 2       | BARN       | 26.01.2010  |
      | 1            | 3       | BARN       | 21.04.2023  |

  Scenario: Skal få brevperiode og brevbegrunnelse med eldste barn flettet inn og korrekt kompetansedata
    Og dagens dato er 19.10.2023
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår                            | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                                             | 11.09.2018 |            | OPPFYLT  | Nei                  |                  |
      | 1       | BOSATT_I_RIKET   | ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING | 11.08.2022 |            | OPPFYLT  | Nei                  |                  |

      | 2       | UNDER_18_ÅR      |                                             | 26.01.2010 | 25.01.2028 | OPPFYLT  | Nei                  |                  |
      | 2       | GIFT_PARTNERSKAP |                                             | 26.01.2010 |            | OPPFYLT  | Nei                  |                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER                    | 11.09.2018 | 04.04.2023 | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS                              | 11.09.2018 | 04.04.2023 | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                                             | 11.09.2018 |            | OPPFYLT  | Nei                  |                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_NORGE_MED_SØKER                  | 05.04.2023 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_NORGE                            | 05.04.2023 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

      | 3       | BOR_MED_SØKER    | BARN_BOR_I_NORGE_MED_SØKER                  | 21.04.2023 |            | OPPFYLT  | Nei                  |                  |
      | 3       | UNDER_18_ÅR      |                                             | 21.04.2023 | 20.04.2041 | OPPFYLT  | Nei                  |                  |
      | 3       | GIFT_PARTNERSKAP |                                             | 21.04.2023 |            | OPPFYLT  | Nei                  |                  |
      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_NORGE                            | 21.04.2023 |            | OPPFYLT  | Nei                  |                  |
      | 3       | LOVLIG_OPPHOLD   |                                             | 21.04.2023 |            | OPPFYLT  | Nei                  |                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.09.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 31.12.2027 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 1            | 01.05.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 3       | 1            | 01.07.2023 | 31.03.2029 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.04.2029 | 31.03.2041 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat            | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.09.2022 | 30.04.2023 | NORGE_ER_PRIMÆRLAND | 1            | MOTTAR_PENSJON   | ARBEIDER                  | LV                    | NO                             | LV                  |
      | 2, 3    | 01.05.2023 |            | NORGE_ER_PRIMÆRLAND | 1            | MOTTAR_PENSJON   | ARBEIDER                  | LV                    | NO                             | NO                  |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                           | Ugyldige begrunnelser |
      | 01.09.2022 | 28.02.2023 | UTBETALING         | EØS_FORORDNINGEN               | INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_STANDARD |                       |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser | Eøsbegrunnelser                                | Fritekster |
      | 01.09.2022 | 28.02.2023 |                      | INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_STANDARD |            |

    Så forvent følgende brevperioder for behandling 1
      | Brevperiodetype | Fra dato       | Til dato         | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |
      | UTBETALING      | september 2022 | til februar 2023 | 1054  | 1                          | 26.01.10            | du                     |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.09.2022 til 28.02.2023
      | Begrunnelse                                    | Type | Barnas fødselsdatoer | Antall barn | Målform | Annen forelders aktivitetsland | Barnets bostedsland | Søkers aktivitetsland | Annen forelders aktivitet | Søkers aktivitet |
      | INNVILGET_SELVSTENDIG_RETT_PRIMÆRLAND_STANDARD | EØS  | 26.01.10             | 1           | NB      | Norge                          | Latvia              | Latvia                | ARBEIDER                  | MOTTAR_PENSJON   |
