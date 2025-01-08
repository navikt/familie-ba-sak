# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser ved delt bosted i vilkår
  Scenario: Ved valg av delt bosted i vilkårsvurderingen så skal det være mulig å velge deltbosted begrunnelse med riktig begrunnelse data

    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | INNVILGET           | FØDSELSHENDELSE  | Ja                        | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | INNVILGET_OG_ENDRET | SØKNAD           | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 23.05.1992  |              |
      | 1            | 3       | BARN       | 24.07.2015  |              |
      | 1            | 4       | BARN       | 28.03.2017  |              |
      | 1            | 5       | BARN       | 06.12.2024  |              |
      | 2            | 1       | SØKER      | 23.05.1992  |              |
      | 2            | 2       | BARN       | 29.08.2008  |              |
      | 2            | 3       | BARN       | 24.07.2015  |              |
      | 2            | 4       | BARN       | 28.03.2017  |              |
      | 2            | 5       | BARN       | 06.12.2024  |              |
    Og dagens dato er 02.01.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 2            | 2       |
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR                                 |                  | 24.07.2015 | 23.07.2033 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP                            |                  | 24.07.2015 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 4       | UNDER_18_ÅR                                 |                  | 28.03.2017 | 27.03.2035 | OPPFYLT  | Nei                  |                      |                  |
      | 4       | GIFT_PARTNERSKAP                            |                  | 28.03.2017 |            | OPPFYLT  | Nei                  |                      |                  |
      | 4       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 5       | UNDER_18_ÅR                                 |                  | 06.12.2024 | 05.12.2042 | OPPFYLT  | Nei                  |                      |                  |
      | 5       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 06.12.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 5       | GIFT_PARTNERSKAP                            |                  | 06.12.2024 |            | OPPFYLT  | Nei                  |                      |                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | UNDER_18_ÅR                                 |                  | 29.08.2008 | 28.08.2026 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP                            |                  | 29.08.2008 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                               | DELT_BOSTED      | 27.03.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR                                 |                  | 24.07.2015 | 23.07.2033 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP                            |                  | 24.07.2015 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 4       | UNDER_18_ÅR                                 |                  | 28.03.2017 | 27.03.2035 | OPPFYLT  | Nei                  |                      |                  |
      | 4       | GIFT_PARTNERSKAP                            |                  | 28.03.2017 |            | OPPFYLT  | Nei                  |                      |                  |
      | 4       | BOR_MED_SØKER,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 5       | BOR_MED_SØKER,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 06.12.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 5       | GIFT_PARTNERSKAP                            |                  | 06.12.2024 |            | OPPFYLT  | Nei                  |                      |                  |
      | 5       | UNDER_18_ÅR                                 |                  | 06.12.2024 | 05.12.2042 | OPPFYLT  | Nei                  |                      |                  |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak       | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 2            | 01.04.2023 | 31.12.2024 | DELT_BOSTED | 0       | 16.12.2024       | 2023-03-27                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3       | 1            | 01.03.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 1            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 1            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 1            | 01.09.2024 | 30.06.2033 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 1            | 01.03.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 4       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 4       | 1            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 4       | 1            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 4       | 1            | 01.09.2024 | 28.02.2035 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 5       | 1            | 01.01.2025 | 30.11.2042 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |

      | 2       | 2            | 01.04.2023 | 31.12.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1083 |
      | 2       | 2            | 01.01.2025 | 31.07.2026 | 883   | ORDINÆR_BARNETRYGD | 50      | 1766 |
      | 3       | 2            | 01.03.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 2            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 2            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 2            | 01.09.2024 | 30.06.2033 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 2            | 01.03.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 4       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 4       | 2            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 4       | 2            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 4       | 2            | 01.09.2024 | 28.02.2035 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 5       | 2            | 01.01.2025 | 30.11.2042 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Når vedtaksperiodene genereres for behandling 2


    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                                                                            | Ugyldige begrunnelser |
      | 01.01.2025 | 31.07.2026 | UTBETALING         |                                | ETTER_ENDRET_UTBETALING_HAR_AVTALE_DELT_BOSTED                                                                  |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                           | Eøsbegrunnelser | Fritekster |
      | 01.01.2025 | 31.07.2026 | ETTER_ENDRET_UTBETALING_HAR_AVTALE_DELT_BOSTED |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.01.2025 til 31.07.2026
      | Begrunnelse                                    | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | ETTER_ENDRET_UTBETALING_HAR_AVTALE_DELT_BOSTED | STANDARD |               | 29.08.08             | 1           | desember 2024                        |         | 883   | 16.12.24         |                         |                             |