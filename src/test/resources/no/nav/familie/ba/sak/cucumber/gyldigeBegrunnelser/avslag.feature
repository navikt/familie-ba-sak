# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for avslag

  Bakgrunn:
    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId |
      | 1            | 1        |                     |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 03.01.1978  |
      | 1            | 2       | BARN       | 16.02.2007  |

  Scenario: Skal ikke krasje ved avslag uten fom- eller tomdato
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                            | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD                    |                  |            |            | IKKE_OPPFYLT | Ja                   |
      | 1       | UTVIDET_BARNETRYGD,BOSATT_I_RIKET |                  | 22.09.2022 |            | OPPFYLT      | Nei                  |

      | 2       | LOVLIG_OPPHOLD                    |                  |            |            | IKKE_OPPFYLT | Ja                   |
      | 2       | GIFT_PARTNERSKAP                  |                  | 16.02.2007 |            | OPPFYLT      | Nei                  |
      | 2       | UNDER_18_ÅR                       |                  | 16.02.2007 | 15.02.2025 | OPPFYLT      | Nei                  |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET      |                  | 22.09.2022 |            | OPPFYLT      | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato | Til dato | Beløp | Ytelse type | Prosent |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | Fra dato | Til dato | BehandlingId | Årsak | Prosent |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato | Til dato | VedtaksperiodeType | Regelverk | Gyldige begrunnelser | Ugyldige begrunnelser |
      |          |          | AVSLAG             |           |                         |                          |


  Scenario: Skal gi avslag for etterfølgende måned selv når avslag varer under en måned

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat        | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                    | DELVIS_INNVILGET_OG_ENDRET | SØKNAD           | Nei                       | NASJONAL            |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 26.03.1977  |              |
      | 1            | 2       | BARN       | 09.01.2012  |              |

    Og følgende dagens dato 25.01.2024
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår            | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser              | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                             | 01.02.2022 |            | OPPFYLT      | Nei                  |                                   | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD            |                             | 16.01.2023 | 31.01.2023 | IKKE_OPPFYLT | Ja                   | AVSLAG_IKKE_FLYTTET_FRA_EKTEFELLE |                  |
      | 1       | UTVIDET_BARNETRYGD            |                             | 01.02.2023 |            | OPPFYLT      | Nei                  |                                   |                  |

      | 2       | UNDER_18_ÅR                   |                             | 09.01.2012 | 08.01.2030 | OPPFYLT      | Nei                  |                                   |                  |
      | 2       | GIFT_PARTNERSKAP              |                             | 09.01.2012 |            | OPPFYLT      | Nei                  |                                   |                  |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                             | 01.02.2022 |            | OPPFYLT      | Nei                  |                                   | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                 |                             | 01.02.2022 | 31.01.2023 | OPPFYLT      | Nei                  |                                   | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED_SKAL_IKKE_DELES | 01.02.2023 |            | OPPFYLT      | Nei                  |                                   | NASJONALE_REGLER |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 1       | 1            | 01.07.2023 | 31.12.2029 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 1            | 01.03.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 1            | 01.01.2024 | 31.12.2029 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser              | Ugyldige begrunnelser |
      | 01.02.2023 | 28.02.2023 | AVSLAG             |                                | AVSLAG_IKKE_FLYTTET_FRA_EKTEFELLE |                       |
      | 01.03.2023 | 30.06.2023 | UTBETALING         |                                |                                   |                       |
      | 01.07.2023 | 31.12.2023 | UTBETALING         |                                |                                   |                       |
      | 01.01.2024 | 31.12.2029 | UTBETALING         |                                |                                   |                       |
      | 01.01.2030 |            | OPPHØR             |                                |                                   |                       |