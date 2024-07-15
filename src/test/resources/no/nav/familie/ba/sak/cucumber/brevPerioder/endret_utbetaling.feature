# language: no
# encoding: UTF-8

Egenskap: Brevperioder: Endret utbetaling

  Bakgrunn:

  Scenario: Skal kun flette inn barn med utbetaling i brevperioden. Barn med andel endret til 0% skal ikke med, såfremt det ikke er delt bosted.
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat         | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | HENLAGT_FEILAKTIG_OPPRETTET | NYE_OPPLYSNINGER | Nei                       | NASJONAL            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 26.12.1985  |
      | 1            | 2       | BARN       | 08.04.2009  |
      | 1            | 3       | BARN       | 23.07.2018  |

    Og dagens dato er 18.10.2023
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                  | 15.06.2023 |            | OPPFYLT  | Nei                  |

      | 2       | UNDER_18_ÅR                                 |                  | 08.04.2009 | 07.04.2027 | OPPFYLT  | Nei                  |
      | 2       | GIFT_PARTNERSKAP                            |                  | 08.04.2009 |            | OPPFYLT  | Nei                  |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |

      | 3       | UNDER_18_ÅR                                 |                  | 23.07.2018 | 22.07.2036 | OPPFYLT  | Nei                  |
      | 3       | GIFT_PARTNERSKAP                            |                  | 23.07.2018 |            | OPPFYLT  | Nei                  |
      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.07.2023 | 31.03.2027 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

      | 3       | 1            | 01.07.2023 | 30.09.2023 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.10.2023 | 30.06.2036 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |

    Og med endrede utbetalinger
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

    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat  | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | INNVILGET_OG_OPPHØRT | SØKNAD           | Nei                       | NASJONAL            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 02.06.1982  |
      | 1            | 2       | BARN       | 24.05.2010  |
      | 1            | 3       | BARN       | 06.09.2011  |

    Og dagens dato er 18.10.2023
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD                                               |                  | 02.06.1982 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                                               |                  | 15.09.2011 | 15.10.2011 | OPPFYLT  | Nei                  |

      | 2       | BOR_MED_SØKER,GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 24.05.2010 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                                  |                  | 24.05.2010 | 23.05.2028 | OPPFYLT  | Nei                  |

      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,GIFT_PARTNERSKAP               |                  | 06.09.2011 |            | OPPFYLT  | Nei                  |
      | 3       | UNDER_18_ÅR                                                  |                  | 06.09.2011 | 05.09.2029 | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER                                                | DELT_BOSTED      | 06.09.2011 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 2       | 1            | 01.10.2011 | 31.10.2011 | 970   | ORDINÆR_BARNETRYGD | 100     | 970  |
      | 3       | 1            | 01.10.2011 | 31.10.2011 | 0     | ORDINÆR_BARNETRYGD | 0       | 970  |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak       | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 3       | 1            | 01.10.2011 | 31.10.2011 | DELT_BOSTED | 0       | 12.10.2023       | 02.02.2011                  |

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

  Scenario: Skal ta med barna det allerede er utbetalt for i periodeteksten hvis det finnes utvidet barnetrygd i perioden
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | EØS                 |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 28.05.1956  |
      | 1            | 2       | BARN       | 26.06.2011  |

    Og dagens dato er 04.12.2023
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår             | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET     | OMFATTET_AV_NORSK_LOVGIVNING | 01.08.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD     |                              | 01.08.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | UTVIDET_BARNETRYGD |                              | 01.08.2023 |            | OPPFYLT  | Nei                  |                      |                  |

      | 2       | UNDER_18_ÅR        |                              | 26.06.2011 | 25.06.2029 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP   |                              | 26.06.2011 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET     | BARN_BOR_I_NORGE             | 01.08.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD     |                              | 01.08.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER      | BARN_BOR_I_NORGE_MED_SØKER   | 01.08.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.09.2023 | 31.05.2029 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 1            | 01.09.2023 | 30.11.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1310 |
      | 2       | 1            | 01.12.2023 | 31.05.2029 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 1            | 01.09.2023 | 30.11.2023 | ALLEREDE_UTBETALT | 0       | 08.08.2023       |                             |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato | Resultat            | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.12.2023 |          | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | EE                             | NO                  |

    Når vedtaksperiodene genereres for behandling 1

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                                         | Eøsbegrunnelser | Fritekster |
      | 01.09.2023 | 30.11.2023 | ENDRET_UTBETALING_SELVSTENDIG_RETT_ETTERBETALING_UTVIDET_DEL |                 |            |

    Så forvent følgende brevperioder for behandling 1
      | Brevperiodetype | Fra dato       | Til dato          | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |
      | UTBETALING      | september 2023 | til november 2023 | 2516  | 1                          | 26.06.11            | du                     |

  Scenario: Skal ikke ta med barn med allerede utbetalt i periodetekst
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | EØS                 |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 15.08.1985  |              |
      | 1            | 2       | BARN       | 22.03.2019  |              |
      | 1            | 3       | BARN       | 21.07.2023  |              |

    Og dagens dato er 29.01.2024
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår                            | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET   | ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING | 24.02.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD   |                                             | 24.02.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | UNDER_18_ÅR      |                                             | 22.03.2019 | 21.03.2037 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP |                                             | 22.03.2019 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER                    | 24.02.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS                              | 24.02.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                                             | 24.02.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_EØS                              | 21.07.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | LOVLIG_OPPHOLD   |                                             | 21.07.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | GIFT_PARTNERSKAP |                                             | 21.07.2023 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER                    | 21.07.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | UNDER_18_ÅR      |                                             | 21.07.2023 | 20.07.2041 | OPPFYLT  | Nei                  |                      |                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 2       | 1            | 01.03.2020 | 31.12.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 2       | 1            | 01.01.2024 | 28.02.2025 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.03.2025 | 28.02.2037 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 1            | 01.08.2023 | 31.12.2023 | 471   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.01.2024 | 30.06.2029 | 0     | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.07.2029 | 30.06.2041 | 0     | ORDINÆR_BARNETRYGD | 100     | 1510 |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 1            | 01.03.2020 | 31.12.2023 | ALLEREDE_UTBETALT | 0       | 12.01.2024       |                             |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2, 3    | 01.01.2024 |            | NORGE_ER_SEKUNDÆRLAND | 1            | I_ARBEID         | ARBEIDER                  | PL                    | NO                             | PL                  |
      | 3       | 01.08.2023 | 31.12.2023 | NORGE_ER_SEKUNDÆRLAND | 1            | I_ARBEID         | ARBEIDER                  | PL                    | NO                             | PL                  |

    Når vedtaksperiodene genereres for behandling 1

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                                    | Eøsbegrunnelser                                  | Fritekster |
      | 01.03.2020 | 31.07.2023 | ENDRET_UTBETALING_ALLEREDE_UTBETALT_FORELDRE_BOR_SAMMEN |                                                  |            |
      | 01.08.2023 | 31.12.2023 |                                                         | INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_STANDARD |            |
      | 01.01.2024 | 28.02.2037 |                                                         | INNVILGET_SELVSTENDIG_RETT_SEKUNDÆRLAND_STANDARD |            |
      | 01.03.2037 | 30.06.2041 |                                                         |                                                  |            |
      | 01.07.2041 |            |                                                         |                                                  |            |

    Så forvent følgende brevperioder for behandling 1
      | Brevperiodetype  | Fra dato    | Til dato          | Beløp | Antall barn med utbetaling | Barnas fødselsdager  | Du eller institusjonen |
      | INGEN_UTBETALING | mars 2020   |                   | 0     | 0                          |                      | du                     |
      | UTBETALING       | august 2023 | til desember 2023 | 471   | 1                          | 21.07.23             | du                     |
      | UTBETALING       | januar 2024 | til februar 2037  | 0     | 2                          | 22.03.19 og 21.07.23 | du                     |