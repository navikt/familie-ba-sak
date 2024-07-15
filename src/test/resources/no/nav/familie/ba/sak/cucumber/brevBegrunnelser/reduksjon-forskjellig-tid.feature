# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser for reduksjon ved forskjellige tidspunkt

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | Nei                       |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | NYE_OPPLYSNINGER | Nei                       |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 16.04.1985  |
      | 1            | 3456    | BARN       | 19.04.2006  |
      | 1            | 5678    | BARN       | 01.10.2007  |
      | 1            | 7890    | BARN       | 13.10.2014  |
      | 2            | 1234    | SØKER      | 16.04.1985  |
      | 2            | 3456    | BARN       | 19.04.2006  |
      | 2            | 5678    | BARN       | 01.10.2007  |
      | 2            | 7890    | BARN       | 13.10.2014  |

  Scenario: Reduksjon ved forskjellige tidspunkt
    Og dagens dato er 12.10.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                       | Fra dato   | Til dato   | Resultat |
      | 1234    | LOVLIG_OPPHOLD,BOSATT_I_RIKET                                | 16.04.1985 |            | OPPFYLT  |
      | 1234    | UTVIDET_BARNETRYGD                                           | 02.09.2021 | 05.01.2022 | OPPFYLT  |

      | 3456    | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER | 19.04.2006 |            | OPPFYLT  |
      | 3456    | UNDER_18_ÅR                                                  | 19.04.2006 | 18.04.2024 | OPPFYLT  |

      | 7890    | GIFT_PARTNERSKAP                                             | 13.10.2014 |            | OPPFYLT  |
      | 7890    | UNDER_18_ÅR                                                  | 13.10.2014 | 12.10.2032 | OPPFYLT  |
      | 7890    | LOVLIG_OPPHOLD,BOR_MED_SØKER,BOSATT_I_RIKET                  | 31.08.2021 |            | OPPFYLT  |

      | 5678    | LOVLIG_OPPHOLD,BOR_MED_SØKER,GIFT_PARTNERSKAP,BOSATT_I_RIKET | 01.10.2007 |            | OPPFYLT  |
      | 5678    | UNDER_18_ÅR                                                  | 01.10.2007 | 30.09.2025 | OPPFYLT  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 3456    | GIFT_PARTNERSKAP,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 19.04.2006 |            | OPPFYLT  | Nei                  |
      | 3456    | UNDER_18_ÅR                                    |                  | 19.04.2006 | 18.04.2024 | OPPFYLT  | Nei                  |
      | 3456    | BOR_MED_SØKER                                  |                  | 19.04.2006 | 12.08.2022 | OPPFYLT  | Nei                  |

      | 5678    | BOR_MED_SØKER                                  |                  | 01.10.2007 | 11.11.2022 | OPPFYLT  | Nei                  |
      | 5678    | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 01.10.2007 |            | OPPFYLT  | Nei                  |
      | 5678    | UNDER_18_ÅR                                    |                  | 01.10.2007 | 30.09.2025 | OPPFYLT  | Nei                  |

      | 7890    | UNDER_18_ÅR                                    |                  | 13.10.2014 | 12.10.2032 | OPPFYLT  | Nei                  |
      | 7890    | GIFT_PARTNERSKAP                               |                  | 13.10.2014 |            | OPPFYLT  | Nei                  |
      | 7890    | LOVLIG_OPPHOLD,BOR_MED_SØKER,BOSATT_I_RIKET    |                  | 31.08.2021 |            | OPPFYLT  | Nei                  |

      | 1234    | BOSATT_I_RIKET,LOVLIG_OPPHOLD                  |                  | 16.04.1985 |            | OPPFYLT  | Nei                  |
      | 1234    | UTVIDET_BARNETRYGD                             |                  | 02.09.2021 | 05.01.2022 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 5678    | 1            | 01.11.2007 | 28.02.2019 | 970   | ORDINÆR_BARNETRYGD | 100     | 970  |
      | 5678    | 1            | 01.03.2019 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5678    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 5678    | 1            | 01.07.2023 | 30.09.2025 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 7890    | 1            | 01.09.2021 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 7890    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 7890    | 1            | 01.07.2023 | 30.09.2032 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 1234    | 1            | 01.10.2021 | 31.01.2022 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 3456    | 1            | 01.05.2006 | 28.02.2019 | 970   | ORDINÆR_BARNETRYGD | 100     | 970  |
      | 3456    | 1            | 01.03.2019 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3456    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3456    | 1            | 01.07.2023 | 31.03.2024 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

      | 5678    | 2            | 01.11.2007 | 28.02.2019 | 970   | ORDINÆR_BARNETRYGD | 100     | 970  |
      | 5678    | 2            | 01.03.2019 | 30.11.2022 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 7890    | 2            | 01.09.2021 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 7890    | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 7890    | 2            | 01.07.2023 | 30.09.2032 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 1234    | 2            | 01.10.2021 | 31.01.2022 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 3456    | 2            | 01.05.2006 | 28.02.2019 | 970   | ORDINÆR_BARNETRYGD | 100     | 970  |
      | 3456    | 2            | 01.03.2019 | 31.08.2022 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser   |
      | 01.09.2022 | 30.11.2022 | REDUKSJON_FLYTTET_BARN |
      | 01.12.2022 | 28.02.2023 | REDUKSJON_FLYTTET_BARN |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.09.2022 til 30.11.2022
      | Begrunnelse            | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | REDUKSJON_FLYTTET_BARN | Nei           | 19.04.06             | 1           | august 2022                          | NB      | 0     |                  | SØKER_HAR_IKKE_RETT     |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.12.2022 til 28.02.2023
      | Begrunnelse            | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | REDUKSJON_FLYTTET_BARN | Nei           | 01.10.07             | 1           | november 2022                        | NB      | 0     |                  | SØKER_HAR_IKKE_RETT     |
