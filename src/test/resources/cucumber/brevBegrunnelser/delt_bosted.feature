# language: no
# encoding: UTF-8

Egenskap: Delt bosted

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | ENDRET_UTBETALING   | SATSENDRING      | Ja                        | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | INNVILGET_OG_ENDRET | SØKNAD           | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 11.01.1989  |              |
      | 1            | 2       | BARN       | 31.10.2016  |              |
      | 1            | 3       | BARN       | 01.10.2018  |              |
      | 2            | 1       | SØKER      | 11.01.1989  |              |
      | 2            | 2       | BARN       | 31.10.2016  |              |
      | 2            | 3       | BARN       | 01.10.2018  |              |

  Scenario: Begrunnelse skal inkludere begge barna ved delt bosted i samme periode uavhengig av periode resultat
    Og dagens dato er 26.09.2024
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 2            | 3       |
      | 2            | 2       |
      | 2            | 1       |
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | GIFT_PARTNERSKAP                            |                  | 31.10.2016 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                                 |                  | 31.10.2016 | 30.10.2034 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR                                 |                  | 01.10.2018 | 30.09.2036 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP                            |                  | 01.10.2018 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | LOVLIG_OPPHOLD,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD            |                  | 21.08.2024 |            | OPPFYLT  | Nei                  |                      |                  |

      | 2       | UNDER_18_ÅR                   |                  | 31.10.2016 | 30.10.2034 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP              |                  | 31.10.2016 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER                 |                  | 01.01.2022 | 20.08.2024 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED      | 21.08.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | GIFT_PARTNERSKAP              |                  | 01.10.2018 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR                   |                  | 01.10.2018 | 30.09.2036 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                 |                  | 01.01.2022 | 20.08.2024 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                 | DELT_BOSTED      | 21.08.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.02.2022 | 30.09.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 1            | 01.10.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 1            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 1            | 01.09.2024 | 30.09.2034 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.02.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 3       | 1            | 01.07.2023 | 30.09.2036 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |

      | 1       | 2            | 01.09.2024 | 30.09.2036 | 1258  | UTVIDET_BARNETRYGD | 50      | 2516 |
      | 2       | 2            | 01.02.2022 | 30.09.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 2            | 01.10.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 2            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 2            | 01.09.2024 | 30.09.2034 | 883   | ORDINÆR_BARNETRYGD | 50      | 1766 |
      | 3       | 2            | 01.02.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 3       | 2            | 01.07.2023 | 31.08.2024 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 2            | 01.09.2024 | 30.09.2036 | 883   | ORDINÆR_BARNETRYGD | 50      | 1766 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                 | Ugyldige begrunnelser |
      | 01.09.2024 | 30.09.2034 | UTBETALING         |                                | INNVILGET_AVTALE_DELT_BOSTED_FÅR_FRA_AVTALETIDSPUNKT |                       |
      | 01.10.2034 | 30.09.2036 | UTBETALING         |                                |                                                      |                       |
      | 01.10.2036 |            | OPPHØR             |                                |                                                      |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                                 | Eøsbegrunnelser | Fritekster |
      | 01.09.2024 | 30.09.2034 | INNVILGET_AVTALE_DELT_BOSTED_FÅR_FRA_AVTALETIDSPUNKT |                 |            |
      | 01.10.2034 | 30.09.2036 |                                                      |                 |            |
      | 01.10.2036 |            |                                                      |                 |            |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.09.2024 til 30.09.2034
      | Begrunnelse                                          | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | INNVILGET_AVTALE_DELT_BOSTED_FÅR_FRA_AVTALETIDSPUNKT | STANDARD |               | 31.10.16 og 01.10.18 | 2           | august 2024                          |         | 1 766 |                  | SØKER_FÅR_UTVIDET        |                             |