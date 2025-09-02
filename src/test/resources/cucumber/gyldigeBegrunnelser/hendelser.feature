# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for hendelser

  Bakgrunn:
    Gitt følgende behandlinger
      | BehandlingId |
      | 1            |

  Scenario: Skal ta med 6-års begrunnelse når barn blir 6 år
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1234    | SØKER      | 11.01.1970  |              |
      | 1            | 3456    | BARN       | 13.04.2017  |              |

    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2017 | 12.04.2035 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2017 |            | Oppfylt  |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2017 | 31.03.2023 | 1354  | 1            |
      | 3456    | 01.04.2023 | 31.03.2035 | 1054  | 1            |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Gyldige begrunnelser | Ugyldige begrunnelser |
      | 01.05.2017 | 31.03.2023 | UTBETALING         |                      |                       |
      | 01.04.2023 | 31.03.2035 | UTBETALING         | REDUKSJON_UNDER_6_ÅR |                       |
      | 01.04.2035 |            | OPPHØR             | OPPHØR_UNDER_18_ÅR   |                       |

  Scenario: Skal ta med dødsfallbegrunnelse om barnet er dødt
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1234    | SØKER      | 11.01.1970  |              |
      | 1            | 5678    | BARN       | 13.04.2017  | 02.03.2024   |

    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 5678    | UNDER_18_ÅR                                                     | 13.04.2017 | 12.04.2035 | Oppfylt  |
      | 5678    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2017 | 02.03.2024 | Oppfylt  |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 5678    | 01.05.2017 | 31.03.2023 | 1354  | 1            |
      | 5678    | 01.04.2023 | 31.03.2024 | 1054  | 1            |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Gyldige begrunnelser | Ugyldige begrunnelser |
      | 01.05.2017 | 31.03.2023 | UTBETALING         |                      |                       |
      | 01.04.2023 | 31.03.2024 | UTBETALING         | REDUKSJON_UNDER_6_ÅR |                       |
      | 01.04.2024 |            | OPPHØR             | OPPHØR_BARN_DØD      |                       |

  Scenario: Skal ta med satsendringbegrunnelse ved satsendring
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1234    | SØKER      | 11.01.1970  |              |
      | 1            | 3456    | BARN       | 13.04.2017  |              |

    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2017 | 12.04.2035 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 13.04.2017 |            | Oppfylt  |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2017 | 28.02.2023 | 1676  | 1            |
      | 3456    | 01.03.2023 | 31.03.2035 | 1083  | 1            |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Gyldige begrunnelser                        | Ugyldige begrunnelser |
      | 01.05.2017 | 28.02.2023 | UTBETALING         |                                             |                       |
      | 01.03.2023 | 31.03.2035 | UTBETALING         | REDUKSJON_UNDER_6_ÅR, REDUKSJON_SATSENDRING |                       |
      | 01.04.2035 |            | OPPHØR             | OPPHØR_UNDER_18_ÅR                          |                       |


  Scenario: Skal bare ta med barnet som har dødsfallsdato i begrunnelsen
    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | INNVILGET           | FØDSELSHENDELSE  | Ja                        | NASJONAL            |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | NYE_OPPLYSNINGER | Nei                       | NASJONAL            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 11.08.1989  |              |
      | 1            | 2       | BARN       | 27.07.2020  |              |
      | 1            | 3       | BARN       | 05.12.2023  |              |
      | 2            | 1       | SØKER      | 11.08.1989  |              |
      | 2            | 2       | BARN       | 27.07.2020  |              |
      | 2            | 3       | BARN       | 05.12.2023  | 29.12.2023   |


    Og dagens dato er 07.01.2024
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | GIFT_PARTNERSKAP                            |                  | 27.07.2020 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                                 |                  | 27.07.2020 | 26.07.2038 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER |                  | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 05.12.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | GIFT_PARTNERSKAP                            |                  | 05.12.2023 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR                                 |                  | 05.12.2023 | 04.12.2041 | OPPFYLT  | Nei                  |                      |                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                      | Utdypende vilkår         | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                          | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | GIFT_PARTNERSKAP                            |                          | 27.07.2020 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                                 |                          | 27.07.2020 | 26.07.2038 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                          | 01.03.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | BOR_MED_SØKER,LOVLIG_OPPHOLD                |                          | 05.12.2023 | 29.12.2023 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | GIFT_PARTNERSKAP,UNDER_18_ÅR                |                          | 05.12.2023 | 29.12.2023 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOSATT_I_RIKET                              | VURDERING_ANNET_GRUNNLAG | 05.12.2023 | 29.12.2023 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.04.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 2       | 1            | 01.07.2023 | 30.06.2026 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.07.2026 | 30.06.2038 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 1            | 01.01.2024 | 30.11.2029 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.12.2029 | 30.11.2041 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

      | 2       | 2            | 01.04.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 2       | 2            | 01.07.2023 | 30.06.2026 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 2            | 01.07.2026 | 30.06.2038 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType                                      | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                     | Ugyldige begrunnelser |
      | 01.01.2024 | 30.06.2026 | UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING |                                | REDUKSJON_BARN_DØDE_SAMME_MÅNED_SOM_FØDT |                       |
      | 01.07.2026 | 30.06.2038 | UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING |                                |                                          |                       |
      | 01.07.2038 |            | OPPHØR                                                  |                                |                                          |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                     | Eøsbegrunnelser | Fritekster |
      | 01.01.2024 | 30.06.2026 | REDUKSJON_BARN_DØDE_SAMME_MÅNED_SOM_FØDT |                 |            |
      | 01.07.2026 | 30.06.2038 |                                          |                 |            |
      | 01.07.2038 |            |                                          |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.01.2024 til 30.06.2026
      | Begrunnelse                              | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp |
      | REDUKSJON_BARN_DØDE_SAMME_MÅNED_SOM_FØDT | STANDARD | false         | 05.12.23             | 1           | desember 2023                        | NB      | 0     |
