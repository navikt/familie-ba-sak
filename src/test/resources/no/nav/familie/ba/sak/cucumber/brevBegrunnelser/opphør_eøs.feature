# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser for opphør av eøs sak

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | ENDRET_UTBETALING   | ÅRLIG_KONTROLL   | Nei                       | EØS                 |
      | 2            | 1        | 1                   | OPPHØRT             | NYE_OPPLYSNINGER | Nei                       | EØS                 |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 21.07.1982  |
      | 1            | 2       | BARN       | 22.01.2006  |
      | 1            | 3       | BARN       | 16.09.2020  |
      | 2            | 1       | SØKER      | 21.07.1982  |
      | 2            | 2       | BARN       | 22.01.2006  |
      | 2            | 3       | BARN       | 16.09.2020  |

  Scenario: Skal flette in alle barna selv om de har forskjellig kompetanse når det er opphør
    Og følgende dagens dato 23.10.2023
    Og lag personresultater for begrunnelse for behandling 1
    Og lag personresultater for begrunnelse for behandling 2

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser |
      | 1       | LOVLIG_OPPHOLD   |                              | 01.12.2020 |            | OPPFYLT  | Nei                  |                      |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 15.05.2023 |            | OPPFYLT  | Nei                  |                      |

      | 2       | UNDER_18_ÅR      |                              | 22.01.2006 | 21.01.2024 | OPPFYLT  | Nei                  |                      |
      | 2       | GIFT_PARTNERSKAP |                              | 22.01.2006 |            | OPPFYLT  | Nei                  |                      |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 01.12.2020 |            | OPPFYLT  | Nei                  |                      |
      | 2       | LOVLIG_OPPHOLD   |                              | 01.12.2020 |            | OPPFYLT  | Nei                  |                      |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 01.12.2020 |            | OPPFYLT  | Nei                  |                      |

      | 3       | GIFT_PARTNERSKAP |                              | 16.09.2020 |            | OPPFYLT  | Nei                  |                      |
      | 3       | UNDER_18_ÅR      |                              | 16.09.2020 | 15.09.2038 | OPPFYLT  | Nei                  |                      |
      | 3       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 01.12.2020 |            | OPPFYLT  | Nei                  |                      |
      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 01.12.2020 |            | OPPFYLT  | Nei                  |                      |
      | 3       | LOVLIG_OPPHOLD   |                              | 01.12.2020 |            | OPPFYLT  | Nei                  |                      |

    Og legg til nye vilkårresultater for begrunnelse for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser |
      | 1       | LOVLIG_OPPHOLD   |                              | 01.12.2020 |            | OPPFYLT  | Nei                  |                      |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 15.05.2023 | 30.06.2023 | OPPFYLT  | Nei                  |                      |

      | 2       | GIFT_PARTNERSKAP |                              | 22.01.2006 |            | OPPFYLT  | Nei                  |                      |
      | 2       | UNDER_18_ÅR      |                              | 22.01.2006 | 21.01.2024 | OPPFYLT  | Nei                  |                      |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 01.12.2020 |            | OPPFYLT  | Nei                  |                      |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 01.12.2020 |            | OPPFYLT  | Nei                  |                      |
      | 2       | LOVLIG_OPPHOLD   |                              | 01.12.2020 |            | OPPFYLT  | Nei                  |                      |

      | 3       | UNDER_18_ÅR      |                              | 16.09.2020 | 15.09.2038 | OPPFYLT  | Nei                  |                      |
      | 3       | GIFT_PARTNERSKAP |                              | 16.09.2020 |            | OPPFYLT  | Nei                  |                      |
      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 01.12.2020 |            | OPPFYLT  | Nei                  |                      |
      | 3       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 01.12.2020 |            | OPPFYLT  | Nei                  |                      |
      | 3       | LOVLIG_OPPHOLD   |                              | 01.12.2020 |            | OPPFYLT  | Nei                  |                      |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.06.2023 | 30.06.2023 | 182   | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 31.12.2023 | 409   | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 1            | 01.06.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 3       | 1            | 01.07.2023 | 31.08.2026 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.09.2026 | 31.08.2038 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

      | 2       | 2            | 01.06.2023 | 30.06.2023 | 182   | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 2            | 01.06.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |

    Og med kompetanser for begrunnelse
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.06.2023 |            | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | LT                             | LT                  |
      | 3       | 01.06.2023 |            | NORGE_ER_PRIMÆRLAND   | 1            | ARBEIDER         | INAKTIV                   | NO                    | LT                             | LT                  |
      | 2       | 01.06.2023 | 30.06.2023 | NORGE_ER_SEKUNDÆRLAND | 2            | ARBEIDER         | I_ARBEID                  | NO                    | LT                             | LT                  |
      | 3       | 01.06.2023 | 30.06.2023 | NORGE_ER_PRIMÆRLAND   | 2            | ARBEIDER         | INAKTIV                   | NO                    | LT                             | LT                  |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser | Ugyldige begrunnelser |
      | 01.07.2023 |          | OPPHØR             | EØS_FORORDNINGEN               | OPPHØR_EØS_STANDARD  |                       |


    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser | Eøsbegrunnelser     | Fritekster |
      | 01.07.2023 |          |                      | OPPHØR_EØS_STANDARD |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.07.2023 til -
      | Begrunnelse         | Type | Barnas fødselsdatoer | Antall barn | Målform | Gjelder søker |
      | OPPHØR_EØS_STANDARD | EØS  | 22.01.06 og 16.09.20 | 2           | NB      | Nei           | 