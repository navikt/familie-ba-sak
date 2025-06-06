# language: no
# encoding: UTF-8

Egenskap: Opphør etter endret utbetaling

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status    |
      | 1        | NORMAL     | AVSLUTTET |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak            | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | FORTSATT_INNVILGET  | OPPDATER_UTVIDET_KLASSEKODE | Ja                        | EØS                 | AVSLUTTET         |
      | 2            | 1        | 1                   | ENDRET_OG_OPPHØRT   | NYE_OPPLYSNINGER            | Nei                       | EØS                 | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 25.06.1983  |              |
      | 1            | 2       | BARN       | 07.09.2013  |              |
      | 1            | 3       | BARN       | 19.04.2015  |              |
      | 1            | 4       | BARN       | 20.12.2018  |              |
      | 2            | 1       | SØKER      | 25.06.1983  |              |
      | 2            | 2       | BARN       | 07.09.2013  |              |
      | 2            | 3       | BARN       | 19.04.2015  |              |
      | 2            | 4       | BARN       | 20.12.2018  |              |

  Scenario: Ved opphør etter at utbetaling allerede er stanset for et eller flere barn pga endret utbetaling skal vi kun flette inn barna som har utbetaling i forrige vedtaksperiode
    Og dagens dato er 21.05.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår             | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD     |                              | 01.10.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET     | OMFATTET_AV_NORSK_LOVGIVNING | 01.10.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | UTVIDET_BARNETRYGD |                              | 07.01.2023 |            | OPPFYLT  | Nei                  |                      |                  |

      | 2       | UNDER_18_ÅR        |                              | 07.09.2013 | 06.09.2031 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP   |                              | 07.09.2013 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET     | BARN_BOR_I_EØS               | 01.10.2022 | 06.01.2023 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD     |                              | 01.10.2022 | 06.01.2023 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER      | BARN_BOR_I_EØS_MED_SØKER     | 01.10.2022 | 06.01.2023 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER      | BARN_BOR_I_NORGE_MED_SØKER   | 07.01.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD     |                              | 07.01.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET     | BARN_BOR_I_NORGE             | 07.01.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 3       | UNDER_18_ÅR        |                              | 19.04.2015 | 18.04.2033 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP   |                              | 19.04.2015 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOSATT_I_RIKET     | BARN_BOR_I_EØS               | 01.10.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | BOR_MED_SØKER      | BARN_BOR_I_EØS_MED_SØKER     | 01.10.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | LOVLIG_OPPHOLD     |                              | 01.10.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 4       | GIFT_PARTNERSKAP   |                              | 20.12.2018 |            | OPPFYLT  | Nei                  |                      |                  |
      | 4       | UNDER_18_ÅR        |                              | 20.12.2018 | 19.12.2036 | OPPFYLT  | Nei                  |                      |                  |
      | 4       | BOR_MED_SØKER      | BARN_BOR_I_NORGE_MED_SØKER   | 01.10.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 4       | BOSATT_I_RIKET     | BARN_BOR_I_EØS               | 01.10.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 4       | LOVLIG_OPPHOLD     |                              | 01.10.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår             | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET     | OMFATTET_AV_NORSK_LOVGIVNING | 01.10.2022 | 30.09.2024 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD     |                              | 01.10.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | UTVIDET_BARNETRYGD |                              | 07.01.2023 |            | OPPFYLT  | Nei                  |                      |                  |

      | 2       | UNDER_18_ÅR        |                              | 07.09.2013 | 06.09.2031 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP   |                              | 07.09.2013 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER      | BARN_BOR_I_EØS_MED_SØKER     | 01.10.2022 | 06.01.2023 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD     |                              | 01.10.2022 | 06.01.2023 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET     | BARN_BOR_I_EØS               | 01.10.2022 | 06.01.2023 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD     |                              | 07.01.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER      | BARN_BOR_I_NORGE_MED_SØKER   | 07.01.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET     | BARN_BOR_I_NORGE             | 07.01.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 3       | GIFT_PARTNERSKAP   |                              | 19.04.2015 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR        |                              | 19.04.2015 | 18.04.2033 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER      | BARN_BOR_I_EØS_MED_SØKER     | 01.10.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | BOSATT_I_RIKET     | BARN_BOR_I_EØS               | 01.10.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | LOVLIG_OPPHOLD     |                              | 01.10.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 4       | UNDER_18_ÅR        |                              | 20.12.2018 | 19.12.2036 | OPPFYLT  | Nei                  |                      |                  |
      | 4       | GIFT_PARTNERSKAP   |                              | 20.12.2018 |            | OPPFYLT  | Nei                  |                      |                  |
      | 4       | BOSATT_I_RIKET     | BARN_BOR_I_EØS               | 01.10.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 4       | BOR_MED_SØKER      | BARN_BOR_I_NORGE_MED_SØKER   | 01.10.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 4       | LOVLIG_OPPHOLD     |                              | 01.10.2022 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat            | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 3, 4, 2 | 01.11.2022 | 31.12.2022 | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | INAKTIV                   | NO                    | NL                             | NL                  |
      | 3, 4    | 01.01.2023 | 30.04.2024 | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | INAKTIV                   | NO                    | NL                             | NL                  |
      | 2       | 01.01.2023 |            | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | INAKTIV                   | NO                    | NL                             | NO                  |
      | 3, 4, 2 | 01.11.2022 | 31.12.2022 | NORGE_ER_PRIMÆRLAND | 2            | ARBEIDER         | INAKTIV                   | NO                    | NL                             | NL                  |
      | 2       | 01.01.2023 | 30.09.2024 | NORGE_ER_PRIMÆRLAND | 2            | ARBEIDER         | INAKTIV                   | NO                    | NL                             | NO                  |
      | 3, 4    | 01.01.2023 | 30.04.2024 | NORGE_ER_PRIMÆRLAND | 2            | ARBEIDER         | INAKTIV                   | NO                    | NL                             | NL                  |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak          | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 4       | 1            | 01.05.2024 | 30.11.2036 | ENDRE_MOTTAKER | 0       | 01.01.2024       |                             |
      | 3       | 1            | 01.05.2024 | 31.03.2033 | ENDRE_MOTTAKER | 0       | 01.01.2024       |                             |
      | 3, 4    | 2            | 01.05.2024 | 30.09.2024 | ENDRE_MOTTAKER | 0       | 01.01.2024       |                             |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.02.2023 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 1            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 1       | 1            | 01.07.2023 | 31.01.2025 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 1            | 01.02.2025 | 30.11.2036 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 1            | 01.11.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 1            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 1            | 01.09.2024 | 31.08.2031 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.11.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 1            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 1            | 01.01.2024 | 30.04.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 1            | 01.05.2024 | 31.03.2033 | 0     | ORDINÆR_BARNETRYGD | 0       | 1510 |
      | 4       | 1            | 01.11.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 4       | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 4       | 1            | 01.07.2023 | 30.04.2024 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 1            | 01.05.2024 | 30.11.2036 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |

      | 1       | 2            | 01.02.2023 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 2            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 1       | 2            | 01.07.2023 | 30.09.2024 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 2            | 01.11.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 2            | 01.01.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 2            | 01.09.2024 | 30.09.2024 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 2            | 01.11.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 2            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 2            | 01.01.2024 | 30.04.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 2            | 01.05.2024 | 30.09.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1510 |
      | 4       | 2            | 01.11.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 4       | 2            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 4       | 2            | 01.07.2023 | 30.04.2024 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 2            | 01.05.2024 | 30.09.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |

    Når vedtaksperiodene genereres for behandling 2


    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser               | Ugyldige begrunnelser |
      | 01.10.2024 |          | OPPHØR             | EØS_FORORDNINGEN               | OPPHØR_IKKE_STATSBORGER_I_EØS_LAND |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser | Eøsbegrunnelser                    | Fritekster |
      | 01.10.2024 |          |                      | OPPHØR_IKKE_STATSBORGER_I_EØS_LAND |            |

    Så forvent følgende brevperioder for behandling 2
      | Brevperiodetype  | Fra dato     | Til dato | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |
      | INGEN_UTBETALING | oktober 2024 |          | 0     | 0                          |                     | du                     |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.10.2024 til null
      | Begrunnelse                        | Type | Barnas fødselsdatoer | Antall barn | Målform | Annen forelders aktivitetsland | Barnets bostedsland | Søkers aktivitetsland | Annen forelders aktivitet | Søkers aktivitet | Gjelder søker |
      | OPPHØR_IKKE_STATSBORGER_I_EØS_LAND | EØS  | 07.09.13             | 1           | NB      |                                |                     |                       |                           |                  | Ja            |