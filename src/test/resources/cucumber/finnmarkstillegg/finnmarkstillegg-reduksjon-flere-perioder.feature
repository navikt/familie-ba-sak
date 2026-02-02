# language: no
# encoding: UTF-8

Egenskap: Finnmarkstillegg reduksjon i flere perioder
  Scenario: Ved reduksjon i flere perioder skal alle begrunnes

    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus  |
      | 1            | 1        |                     | ENDRET_UTBETALING   | SATSENDRING      | Ja                        | NASJONAL            | AVSLUTTET          |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | FINNMARKSTILLEGG | Ja                        | NASJONAL            | IVERKSETTER_VEDTAK |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 17.05.1988  |              |
      | 1            | 2       | BARN       | 19.03.2005  |              |
      | 1            | 3       | BARN       | 15.06.2006  |              |
      | 1            | 4       | BARN       | 02.05.2009  |              |
      | 1            | 5       | BARN       | 03.05.2012  |              |
      | 1            | 6       | BARN       | 04.01.2017  |              |
      | 1            | 7       | BARN       | 17.08.2022  |              |
      | 2            | 1       | SØKER      | 17.05.1988  |              |
      | 2            | 2       | BARN       | 19.03.2005  |              |
      | 2            | 3       | BARN       | 15.06.2006  |              |
      | 2            | 4       | BARN       | 02.05.2009  |              |
      | 2            | 5       | BARN       | 03.05.2012  |              |
      | 2            | 6       | BARN       | 04.01.2017  |              |
      | 2            | 7       | BARN       | 17.08.2022  |              |
    Og dagens dato er 23.01.2026
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET               |                              | 01.01.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | LOVLIG_OPPHOLD               |                              | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 01.09.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | UNDER_18_ÅR                  |                              | 19.03.2005 | 18.03.2023 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP             |                              | 19.03.2005 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER,LOVLIG_OPPHOLD |                              | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET               |                              | 01.01.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 01.09.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR                  |                              | 15.06.2006 | 14.06.2024 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP             |                              | 15.06.2006 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | LOVLIG_OPPHOLD,BOR_MED_SØKER |                              | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOSATT_I_RIKET               |                              | 01.01.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 01.09.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 4       | UNDER_18_ÅR                  |                              | 02.05.2009 | 01.05.2027 | OPPFYLT  | Nei                  |                      |                  |
      | 4       | GIFT_PARTNERSKAP             |                              | 02.05.2009 |            | OPPFYLT  | Nei                  |                      |                  |
      | 4       | BOR_MED_SØKER,LOVLIG_OPPHOLD |                              | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BOSATT_I_RIKET               |                              | 01.01.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 01.09.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 5       | UNDER_18_ÅR                  |                              | 03.05.2012 | 02.05.2030 | OPPFYLT  | Nei                  |                      |                  |
      | 5       | GIFT_PARTNERSKAP             |                              | 03.05.2012 |            | OPPFYLT  | Nei                  |                      |                  |
      | 5       | BOSATT_I_RIKET               |                              | 01.01.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 5       | BOR_MED_SØKER,LOVLIG_OPPHOLD |                              | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 5       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 01.09.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 6       | GIFT_PARTNERSKAP             |                              | 04.01.2017 |            | OPPFYLT  | Nei                  |                      |                  |
      | 6       | UNDER_18_ÅR                  |                              | 04.01.2017 | 03.01.2035 | OPPFYLT  | Nei                  |                      |                  |
      | 6       | BOSATT_I_RIKET               |                              | 01.01.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 6       | LOVLIG_OPPHOLD,BOR_MED_SØKER |                              | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 6       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 01.09.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 7       | GIFT_PARTNERSKAP             |                              | 17.08.2022 |            | OPPFYLT  | Nei                  |                      |                  |
      | 7       | BOR_MED_SØKER,LOVLIG_OPPHOLD |                              | 17.08.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 7       | BOSATT_I_RIKET               |                              | 17.08.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 7       | UNDER_18_ÅR                  |                              | 17.08.2022 | 16.08.2040 | OPPFYLT  | Nei                  |                      |                  |
      | 7       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 01.09.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET               |                              | 01.01.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | LOVLIG_OPPHOLD               |                              | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 01.09.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | UNDER_18_ÅR                  |                              | 19.03.2005 | 18.03.2023 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP             |                              | 19.03.2005 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER,LOVLIG_OPPHOLD |                              | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET               |                              | 01.01.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 01.09.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR                  |                              | 15.06.2006 | 14.06.2024 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP             |                              | 15.06.2006 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER,LOVLIG_OPPHOLD |                              | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOSATT_I_RIKET               |                              | 01.01.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 01.09.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 4       | UNDER_18_ÅR                  |                              | 02.05.2009 | 01.05.2027 | OPPFYLT  | Nei                  |                      |                  |
      | 4       | GIFT_PARTNERSKAP             |                              | 02.05.2009 |            | OPPFYLT  | Nei                  |                      |                  |
      | 4       | BOR_MED_SØKER,LOVLIG_OPPHOLD |                              | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BOSATT_I_RIKET               |                              | 01.01.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 01.09.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 5       | UNDER_18_ÅR                  |                              | 03.05.2012 | 02.05.2030 | OPPFYLT  | Nei                  |                      |                  |
      | 5       | GIFT_PARTNERSKAP             |                              | 03.05.2012 |            | OPPFYLT  | Nei                  |                      |                  |
      | 5       | BOR_MED_SØKER,LOVLIG_OPPHOLD |                              | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 5       | BOSATT_I_RIKET               |                              | 01.01.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 5       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 01.09.2025 | 07.12.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 5       | BOSATT_I_RIKET               |                              | 08.12.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 6       | UNDER_18_ÅR                  |                              | 04.01.2017 | 03.01.2035 | OPPFYLT  | Nei                  |                      |                  |
      | 6       | GIFT_PARTNERSKAP             |                              | 04.01.2017 |            | OPPFYLT  | Nei                  |                      |                  |
      | 6       | BOR_MED_SØKER,LOVLIG_OPPHOLD |                              | 01.01.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 6       | BOSATT_I_RIKET               |                              | 01.01.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 6       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 01.09.2025 | 07.12.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 6       | BOSATT_I_RIKET               |                              | 08.12.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 7       | UNDER_18_ÅR                  |                              | 17.08.2022 | 16.08.2040 | OPPFYLT  | Nei                  |                      |                  |
      | 7       | BOR_MED_SØKER,LOVLIG_OPPHOLD |                              | 17.08.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 7       | GIFT_PARTNERSKAP             |                              | 17.08.2022 |            | OPPFYLT  | Nei                  |                      |                  |
      | 7       | BOSATT_I_RIKET               |                              | 17.08.2022 | 31.08.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 7       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 01.09.2025 | 07.12.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 7       | BOSATT_I_RIKET               |                              | 08.12.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.02.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.02.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 1            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 1            | 01.01.2024 | 31.05.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 4       | 1            | 01.02.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 4       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 4       | 1            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 4       | 1            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 4       | 1            | 01.09.2024 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 1            | 01.05.2025 | 31.01.2026 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 4       | 1            | 01.10.2025 | 31.01.2026 | 500   | FINNMARKSTILLEGG   | 100     | 500  |
      | 4       | 1            | 01.02.2026 | 30.04.2027 | 512   | FINNMARKSTILLEGG   | 100     | 512  |
      | 4       | 1            | 01.02.2026 | 30.04.2027 | 2012  | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 5       | 1            | 01.02.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 5       | 1            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 5       | 1            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 5       | 1            | 01.09.2024 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 5       | 1            | 01.05.2025 | 31.01.2026 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 5       | 1            | 01.10.2025 | 31.01.2026 | 500   | FINNMARKSTILLEGG   | 100     | 500  |
      | 5       | 1            | 01.02.2026 | 30.04.2030 | 2012  | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 5       | 1            | 01.02.2026 | 30.04.2030 | 512   | FINNMARKSTILLEGG   | 100     | 512  |
      | 6       | 1            | 01.02.2022 | 31.12.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 6       | 1            | 01.01.2023 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 6       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 6       | 1            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 6       | 1            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 6       | 1            | 01.09.2024 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 6       | 1            | 01.05.2025 | 31.01.2026 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 6       | 1            | 01.10.2025 | 31.01.2026 | 500   | FINNMARKSTILLEGG   | 100     | 500  |
      | 6       | 1            | 01.02.2026 | 31.12.2034 | 2012  | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 6       | 1            | 01.02.2026 | 31.12.2034 | 512   | FINNMARKSTILLEGG   | 100     | 512  |
      | 7       | 1            | 01.09.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 7       | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 7       | 1            | 01.07.2023 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 7       | 1            | 01.05.2025 | 31.01.2026 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 7       | 1            | 01.10.2025 | 31.01.2026 | 500   | FINNMARKSTILLEGG   | 100     | 500  |
      | 7       | 1            | 01.02.2026 | 31.07.2040 | 2012  | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 7       | 1            | 01.02.2026 | 31.07.2040 | 512   | FINNMARKSTILLEGG   | 100     | 512  |

      | 2       | 2            | 01.02.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 2            | 01.02.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 2            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 2            | 01.01.2024 | 31.05.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 4       | 2            | 01.02.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 4       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 4       | 2            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 4       | 2            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 4       | 2            | 01.09.2024 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 2            | 01.05.2025 | 31.01.2026 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 4       | 2            | 01.10.2025 | 31.01.2026 | 500   | FINNMARKSTILLEGG   | 100     | 500  |
      | 4       | 2            | 01.02.2026 | 30.04.2027 | 2012  | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 4       | 2            | 01.02.2026 | 30.04.2027 | 512   | FINNMARKSTILLEGG   | 100     | 512  |
      | 5       | 2            | 01.02.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 5       | 2            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 5       | 2            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 5       | 2            | 01.09.2024 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 5       | 2            | 01.05.2025 | 31.01.2026 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 5       | 2            | 01.10.2025 | 31.12.2025 | 500   | FINNMARKSTILLEGG   | 100     | 500  |
      | 5       | 2            | 01.02.2026 | 30.04.2030 | 2012  | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 6       | 2            | 01.02.2022 | 31.12.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 6       | 2            | 01.01.2023 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 6       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 6       | 2            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 6       | 2            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 6       | 2            | 01.09.2024 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 6       | 2            | 01.05.2025 | 31.01.2026 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 6       | 2            | 01.10.2025 | 31.12.2025 | 500   | FINNMARKSTILLEGG   | 100     | 500  |
      | 6       | 2            | 01.02.2026 | 31.12.2034 | 2012  | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 7       | 2            | 01.09.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 7       | 2            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 7       | 2            | 01.07.2023 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 7       | 2            | 01.05.2025 | 31.01.2026 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 7       | 2            | 01.10.2025 | 31.12.2025 | 500   | FINNMARKSTILLEGG   | 100     | 500  |
      | 7       | 2            | 01.02.2026 | 31.07.2040 | 2012  | ORDINÆR_BARNETRYGD | 100     | 2012 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser       | Ugyldige begrunnelser |
      | 01.01.2026 | 31.01.2026 | UTBETALING         |                                | REDUKSJON_FINNMARKSTILLEGG |                       |
      | 01.02.2026 | 30.04.2027 | UTBETALING         |                                | REDUKSJON_FINNMARKSTILLEGG |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser       | Eøsbegrunnelser | Fritekster |
      | 01.01.2026 | 31.01.2026 | REDUKSJON_FINNMARKSTILLEGG |                 |            |
      | 01.02.2026 | 30.04.2027 | REDUKSJON_FINNMARKSTILLEGG |                 |            |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.01.2026 til 31.01.2026
      | Begrunnelse                | Type     | Gjelder søker | Barnas fødselsdatoer           | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | REDUKSJON_FINNMARKSTILLEGG | STANDARD |               | 03.05.12, 04.01.17 og 17.08.22 | 3           | desember 2025                        |         | 5 904 |                  |                         |                             |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.02.2026 til 30.04.2027
      | Begrunnelse                | Type     | Gjelder søker | Barnas fødselsdatoer           | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | REDUKSJON_FINNMARKSTILLEGG | STANDARD |               | 03.05.12, 04.01.17 og 17.08.22 | 3           | januar 2026                          |         | 6 036 |                  |                         |                             |