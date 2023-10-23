# language: no
# encoding: UTF-8

Egenskap: Brevperioder: Endret utbetaling

  Bakgrunn:

  Scenario: Skal kun flette inn barn med utbetaling i brevperioden. Barn med andel endret til 0% skal ikke med, såfremt det ikke er delt bosted.
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat         | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | HENLAGT_FEILAKTIG_OPPRETTET | NYE_OPPLYSNINGER | Nei                       | NASJONAL            |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 26.12.1985  |
      | 1            | 2       | BARN       | 08.04.2009  |
      | 1            | 3       | BARN       | 23.07.2018  |

    Og følgende dagens dato 18.10.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                  | 15.06.2023 |            | OPPFYLT  | Nei                  |

      | 2       | UNDER_18_ÅR                                 |                  | 08.04.2009 | 07.04.2027 | OPPFYLT  | Nei                  |
      | 2       | GIFT_PARTNERSKAP                            |                  | 08.04.2009 |            | OPPFYLT  | Nei                  |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |

      | 3       | UNDER_18_ÅR                                 |                  | 23.07.2018 | 22.07.2036 | OPPFYLT  | Nei                  |
      | 3       | GIFT_PARTNERSKAP                            |                  | 23.07.2018 |            | OPPFYLT  | Nei                  |
      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.07.2023 | 31.03.2027 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

      | 3       | 1            | 01.07.2023 | 30.09.2023 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.10.2023 | 30.06.2036 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak          | Prosent | Søknadstidspunkt |
      | 3       | 1            | 01.10.2023 | 30.06.2036 | ENDRE_MOTTAKER | 0       | 28.08.2023       |

    Når vedtaksperiodene genereres for behandling 1

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                       | Eøsbegrunnelser | Fritekster |
      | 01.10.2023 | 31.03.2027 | ENDRET_UTBETALING_REDUKSJON_ENDRE_MOTTAKER |                 |            |

    Så forvent følgende brevperioder for behandling 1
      | Brevperiodetype | Fra dato     | Til dato      | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |
      | UTBETALING      | oktober 2023 | til mars 2027 | 1310  | 1                          | 08.04.09            | du                     |

  Scenario:  Skal flette inn barn i periode selv om de har andeler endret til 0% når det er delt bosted

    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat  | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | INNVILGET_OG_OPPHØRT | SØKNAD           | Nei                       | NASJONAL            |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 02.06.1982  |
      | 1            | 2       | BARN       | 24.05.2010  |
      | 1            | 3       | BARN       | 06.09.2011  |

    Og følgende dagens dato 18.10.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD                                               |                  | 02.06.1982 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                                               |                  | 15.09.2011 | 15.10.2011 | OPPFYLT  | Nei                  |

      | 2       | BOR_MED_SØKER,GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 24.05.2010 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                                  |                  | 24.05.2010 | 23.05.2028 | OPPFYLT  | Nei                  |

      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,GIFT_PARTNERSKAP               |                  | 06.09.2011 |            | OPPFYLT  | Nei                  |
      | 3       | UNDER_18_ÅR                                                  |                  | 06.09.2011 | 05.09.2029 | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER                                                | DELT_BOSTED      | 06.09.2011 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 2       | 1            | 01.10.2011 | 31.10.2011 | 970   | ORDINÆR_BARNETRYGD | 100     | 970  |
      | 3       | 1            | 01.10.2011 | 31.10.2011 | 0     | ORDINÆR_BARNETRYGD | 0       | 970  |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak       | Prosent | Søknadstidspunkt |
      | 3       | 1            | 01.10.2011 | 31.10.2011 | DELT_BOSTED | 0       | 12.10.2023       |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                      | Ugyldige begrunnelser |
      | 01.10.2011 | 31.10.2011 | UTBETALING         |                                | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY |                       |
      | 01.11.2011 |            | OPPHØR             |                                |                                                           |                       |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                                      | Eøsbegrunnelser | Fritekster |
      | 01.10.2011 | 31.10.2011 | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY |                 |            |

    Så forvent følgende brevperioder for behandling 1
      | Brevperiodetype | Fra dato     | Til dato         | Beløp | Antall barn med utbetaling | Barnas fødselsdager  | Du eller institusjonen |
      | UTBETALING      | oktober 2011 | til oktober 2011 | 970   | 2                          | 24.05.10 og 06.09.11 | du                     |