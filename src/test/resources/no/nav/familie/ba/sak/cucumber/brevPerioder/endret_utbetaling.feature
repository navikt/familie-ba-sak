# language: no
# encoding: UTF-8

Egenskap: Brevperioder: Endret utbetaling

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat         | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | ENDRET_UTBETALING           | SATSENDRING      | Ja                        | NASJONAL            |
      | 2            | 1        | 1                   | HENLAGT_FEILAKTIG_OPPRETTET | NYE_OPPLYSNINGER | Nei                       | NASJONAL            |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 26.12.1985  |
      | 1            | 2       | BARN       | 08.04.2009  |
      | 1            | 3       | BARN       | 23.07.2018  |
      | 1            | 4       | BARN       | 08.09.2022  |
      | 2            | 1       | SØKER      | 26.12.1985  |
      | 2            | 2       | BARN       | 08.04.2009  |
      | 2            | 3       | BARN       | 23.07.2018  |
      | 2            | 4       | BARN       | 08.09.2022  |

  Scenario: Skal kun flette inn barn med utbetaling i brevperioden. Barn med andel endret til 0% skal ikke med, såfremt det ikke er delt bosted.
    Og følgende dagens dato 18.10.2023
    Og lag personresultater for begrunnelse for behandling 1
    Og lag personresultater for begrunnelse for behandling 2

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD                                |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |

      | 2       | UNDER_18_ÅR                                                  |                  | 08.04.2009 | 07.04.2027 | OPPFYLT  | Nei                  |
      | 2       | GIFT_PARTNERSKAP                                             |                  | 08.04.2009 |            | OPPFYLT  | Nei                  |
      | 2       | LOVLIG_OPPHOLD,BOR_MED_SØKER,BOSATT_I_RIKET                  |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |

      | 3       | UNDER_18_ÅR                                                  |                  | 23.07.2018 | 22.07.2036 | OPPFYLT  | Nei                  |
      | 3       | GIFT_PARTNERSKAP                                             |                  | 23.07.2018 |            | OPPFYLT  | Nei                  |
      | 3       | BOSATT_I_RIKET,BOR_MED_SØKER,LOVLIG_OPPHOLD                  |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |

      | 4       | GIFT_PARTNERSKAP,BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER |                  | 08.09.2022 |            | OPPFYLT  | Nei                  |
      | 4       | UNDER_18_ÅR                                                  |                  | 08.09.2022 | 07.09.2040 | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for begrunnelse for behandling 2
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET                                |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |

      | 2       | UNDER_18_ÅR                                                  |                  | 08.04.2009 | 07.04.2027 | OPPFYLT  | Nei                  |
      | 2       | GIFT_PARTNERSKAP                                             |                  | 08.04.2009 |            | OPPFYLT  | Nei                  |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER                  |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |

      | 3       | UNDER_18_ÅR                                                  |                  | 23.07.2018 | 22.07.2036 | OPPFYLT  | Nei                  |
      | 3       | GIFT_PARTNERSKAP                                             |                  | 23.07.2018 |            | OPPFYLT  | Nei                  |
      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER                  |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |

      | 4       | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 08.09.2022 |            | OPPFYLT  | Nei                  |
      | 4       | UNDER_18_ÅR                                                  |                  | 08.09.2022 | 07.09.2040 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.03.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 31.03.2027 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 1            | 01.03.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 3       | 1            | 01.07.2023 | 30.06.2024 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.07.2024 | 30.06.2036 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 4       | 1            | 01.10.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 4       | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 4       | 1            | 01.07.2023 | 31.08.2028 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 1            | 01.09.2028 | 31.08.2040 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

      | 2       | 2            | 01.03.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.07.2023 | 31.03.2027 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 2            | 01.03.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 3       | 2            | 01.07.2023 | 30.09.2023 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 2            | 01.10.2023 | 30.06.2036 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 4       | 2            | 01.10.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 4       | 2            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 4       | 2            | 01.07.2023 | 31.08.2028 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 2            | 01.09.2028 | 31.08.2040 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak          | Prosent | Søknadstidspunkt |
      | 3       | 2            | 01.10.2023 | 30.06.2036 | ENDRE_MOTTAKER | 0       | 28.08.2023       |

    Når vedtaksperiodene genereres for behandling 2

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                       | Eøsbegrunnelser | Fritekster |
      | 01.10.2023 | 31.03.2027 | ENDRET_UTBETALING_REDUKSJON_ENDRE_MOTTAKER |                 |            |

    Så forvent følgende brevperioder for behandling 2
      | Brevperiodetype | Fra dato     | Til dato      | Beløp | Antall barn med utbetaling | Barnas fødselsdager  | Du eller institusjonen |
      | UTBETALING      | oktober 2023 | til mars 2027 | 3076  | 2                          | 08.04.09 og 08.09.22 | du                     |