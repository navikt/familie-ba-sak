# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for opphør fra forrige behandling eøs

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
      | 1            | 1       | SØKER      | 13.09.1976  |
      | 1            | 2       | BARN       | 06.10.2022  |
      | 2            | 1       | SØKER      | 13.09.1976  |
      | 2            | 2       | BARN       | 06.10.2022  |

  Scenario: Skal gi opphør når det viser seg at søker ikke bodde i eøs i forrige behandling
    Og dagens dato er 22.11.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                              | 01.01.2023 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 01.05.2023 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

      | 2       | GIFT_PARTNERSKAP |                              | 06.10.2022 |            | OPPFYLT  | Nei                  |                  |
      | 2       | UNDER_18_ÅR      |                              | 06.10.2022 | 05.10.2040 | OPPFYLT  | Nei                  |                  |
      | 2       | LOVLIG_OPPHOLD   |                              | 01.05.2023 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 01.05.2023 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 01.05.2023 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår         | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                          | 01.01.2023 |            | OPPFYLT      | Nei                  | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   |                          | 01.05.2023 |            | IKKE_OPPFYLT | Nei                  | EØS_FORORDNINGEN |

      | 2       | GIFT_PARTNERSKAP |                          | 06.10.2022 |            | OPPFYLT      | Nei                  |                  |
      | 2       | UNDER_18_ÅR      |                          | 06.10.2022 | 05.10.2040 | OPPFYLT      | Nei                  |                  |
      | 2       | LOVLIG_OPPHOLD   |                          | 01.05.2023 |            | OPPFYLT      | Nei                  | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS           | 01.05.2023 |            | OPPFYLT      | Nei                  | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER | 01.05.2023 |            | OPPFYLT      | Nei                  | EØS_FORORDNINGEN |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.06.2023 | 30.06.2023 | 1513  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 2       | 1            | 01.07.2023 | 30.09.2028 | 1556  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.10.2028 | 30.09.2040 | 1100  | ORDINÆR_BARNETRYGD | 100     | 1310 |


    Og med kompetanser
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.06.2023 |          | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | INAKTIV                   | BG                    | NO                             | BG                  |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                     | Ugyldige begrunnelser |
      | 01.06.2023 |          | OPPHØR             | EØS_FORORDNINGEN               | OPPHOR_SELVSTENDIG_RETT_OPPHOR_FRA_START |                       |
