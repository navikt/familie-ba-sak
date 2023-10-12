# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser ved reduksjon EØS

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | Nei                       |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 5678    | BARN       | 06.04.2006  |
      | 1            | 3456    | BARN       | 09.04.2005  |
      | 1            | 1234    | SØKER      | 09.05.1988  |
      | 1            | 7890    | BARN       | 24.06.2010  |

  Scenario: Frafall av andeler og ikke kompetanse
    Og følgende dagens dato 11.10.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat |
      | 7890    | UNDER_18_ÅR      |                              | 24.06.2010 | 23.06.2028 | OPPFYLT  |
      | 7890    | GIFT_PARTNERSKAP |                              | 24.06.2010 |            | OPPFYLT  |
      | 7890    | LOVLIG_OPPHOLD   |                              | 11.11.2022 |            | OPPFYLT  |
      | 7890    | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 11.11.2022 |            | OPPFYLT  |
      | 7890    | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 11.11.2022 |            | OPPFYLT  |

      | 1234    | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 11.11.2022 |            | OPPFYLT  |
      | 1234    | LOVLIG_OPPHOLD   |                              | 11.11.2022 |            | OPPFYLT  |

      | 3456    | UNDER_18_ÅR      |                              | 09.04.2005 | 08.04.2023 | OPPFYLT  |
      | 3456    | GIFT_PARTNERSKAP |                              | 09.04.2005 |            | OPPFYLT  |
      | 3456    | LOVLIG_OPPHOLD   |                              | 11.11.2022 |            | OPPFYLT  |
      | 3456    | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 11.11.2022 |            | OPPFYLT  |
      | 3456    | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 11.11.2022 |            | OPPFYLT  |

      | 5678    | UNDER_18_ÅR      |                              | 06.04.2006 | 05.04.2024 | OPPFYLT  |
      | 5678    | GIFT_PARTNERSKAP |                              | 06.04.2006 |            | OPPFYLT  |
      | 5678    | LOVLIG_OPPHOLD   |                              | 11.11.2022 |            | OPPFYLT  |
      | 5678    | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 11.11.2022 |            | OPPFYLT  |
      | 5678    | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 11.11.2022 |            | OPPFYLT  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3456    | 1            | 01.12.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3456    | 1            | 01.03.2023 | 31.03.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |

      | 5678    | 1            | 01.12.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5678    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 5678    | 1            | 01.07.2023 | 31.03.2024 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

      | 7890    | 1            | 01.12.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 7890    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 7890    | 1            | 01.07.2023 | 31.05.2028 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med kompetanser for begrunnelse
      | AktørId          | Fra dato   | Til dato   | Resultat            | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 7890, 3456, 5678 | 01.12.2022 | 31.03.2023 | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | DK                             | DK                  |
      | 7890, 5678       | 01.04.2023 |            | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | DK                             | DK                  |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Inkluderte Begrunnelser | Inkluderte Begrunnelser        | Ekskluderte Begrunnelser |
      | 01.04.2023 | 30.06.2023 | UTBETALING         | EØS_FORORDNINGEN                  | REDUKSJON_IKKE_ANSVAR_FOR_BARN |                          |

    Og med vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser | Eøsbegrunnelser                | Fritekster |
      | 01.04.2023 | 30.06.2023 |                      | REDUKSJON_IKKE_ANSVAR_FOR_BARN |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.04.2023 til 30.06.2023
      | Begrunnelse                    | Type | Barnas fødselsdatoer | Antall barn | Målform | Annen forelders aktivitetsland | Barnets bostedsland | Søkers aktivitetsland | Annen forelders aktivitet | Søkers aktivitet |
      | REDUKSJON_IKKE_ANSVAR_FOR_BARN | EØS  | 09.04.05             | 1           | NB      | Danmark                        | Danmark             | Norge                 | I_ARBEID                  | ARBEIDER         |
