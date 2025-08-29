# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser ved annenpart

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk |
      | 1            | 1        |                     | ENDRET_UTBETALING   | SMÅBARNSTILLEGG  | Ja                        |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | SMÅBARNSTILLEGG  | Nei                       |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | ANNENPART  | 25.09.1983  |
      | 1            | 2       | SØKER      | 08.09.1995  |
      | 1            | 3       | BARN       | 24.12.2021  |
      | 2            | 2       | SØKER      | 08.09.1995  |
      | 2            | 3       | BARN       | 24.12.2021  |

  Scenario: Skal kunne lage gyldige begrunnelser når vi har annenpart uten data
    Og dagens dato er 17.10.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET                                |                  | 01.12.2021 | 28.02.2023 | OPPFYLT  | Nei                  |
      | 2       | UTVIDET_BARNETRYGD                                           |                  | 24.12.2021 |            | OPPFYLT  | Nei                  |

      | 3       | BOSATT_I_RIKET,GIFT_PARTNERSKAP,BOR_MED_SØKER,LOVLIG_OPPHOLD |                  | 24.12.2021 |            | OPPFYLT  | Nei                  |
      | 3       | UNDER_18_ÅR                                                  |                  | 24.12.2021 | 23.12.2039 | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD                                |                  | 01.12.2021 | 28.01.2023 | OPPFYLT  | Nei                  |
      | 2       | UTVIDET_BARNETRYGD                                           |                  | 24.12.2021 |            | OPPFYLT  | Nei                  |

      | 3       | UNDER_18_ÅR                                                  |                  | 24.12.2021 | 23.12.2039 | OPPFYLT  | Nei                  |
      | 3       | LOVLIG_OPPHOLD,BOR_MED_SØKER,BOSATT_I_RIKET,GIFT_PARTNERSKAP |                  | 24.12.2021 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.01.2022 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.01.2022 | 28.02.2023 | 660   | SMÅBARNSTILLEGG    | 100     | 660  |
      | 3       | 1            | 01.01.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |

      | 2       | 2            | 01.01.2022 | 31.01.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.01.2022 | 31.01.2023 | 660   | SMÅBARNSTILLEGG    | 100     | 660  |
      | 3       | 2            | 01.01.2022 | 31.01.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk | Gyldige begrunnelser | Ugyldige begrunnelser |
      | 01.02.2023 |          | OPPHØR             |           | OPPHØR_UTVANDRET     |                       |