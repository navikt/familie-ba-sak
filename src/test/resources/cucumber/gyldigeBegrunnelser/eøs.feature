# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser ved EØS

  Scenario: Skal gi innvilget begrunnelser for eøs når kompetanse endrer seg, selv om det er reduksjon for person
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | EØS                 |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 12.03.1994  |
      | 1            | 2       | BARN       | 01.01.2017  |

    Og dagens dato er 27.11.2023
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                              | 01.08.2019 |            | OPPFYLT  | Nei                  |                      |                  |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 15.11.2022 | 15.01.2023 | OPPFYLT  | Nei                  |                      |                  |

      | 2       | GIFT_PARTNERSKAP |                              | 01.01.2017 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR      |                              | 01.01.2017 | 31.12.2034 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 01.08.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 01.08.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 01.08.2019 |            | OPPFYLT  | Nei                  |                      |                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.12.2022 | 31.12.2022 | 1414  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 1            | 01.01.2023 | 31.01.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.12.2022 | 31.12.2022 | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | LV                             | LV                  |
      | 2       | 01.01.2023 | 31.01.2023 | NORGE_ER_PRIMÆRLAND   | 1            | ARBEIDER         | INAKTIV                   | NO                    | LV                             | LV                  |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser          | Ugyldige begrunnelser |
      | 01.01.2023 | 31.01.2023 | UTBETALING         | NASJONALE_REGLER               | REDUKSJON_UNDER_6_ÅR          |                       |
      | 01.01.2023 | 31.01.2023 | UTBETALING         | EØS_FORORDNINGEN               | INNVILGET_PRIMÆRLAND_STANDARD |                       |
