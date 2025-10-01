# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser ved reduksjon

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | Nei                       |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 5678    | BARN       | 06.04.2006  |
      | 1            | 3456    | BARN       | 09.04.2005  |
      | 1            | 1234    | SØKER      | 09.05.1988  |
      | 1            | 7890    | BARN       | 24.06.2010  |

  Scenario: Frafall av andeler og ikke kompetanse
    Og dagens dato er 11.10.2023
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Vurderes etter   |
      | 7890    | UNDER_18_ÅR      |                              | 24.06.2010 | 23.06.2028 | OPPFYLT  |                  |
      | 7890    | GIFT_PARTNERSKAP |                              | 24.06.2010 |            | OPPFYLT  |                  |
      | 7890    | LOVLIG_OPPHOLD   |                              | 11.11.2022 |            | OPPFYLT  | EØS_FORORDNINGEN |
      | 7890    | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 11.11.2022 |            | OPPFYLT  | EØS_FORORDNINGEN |
      | 7890    | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 11.11.2022 |            | OPPFYLT  | EØS_FORORDNINGEN |

      | 1234    | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 11.11.2022 |            | OPPFYLT  | EØS_FORORDNINGEN |
      | 1234    | LOVLIG_OPPHOLD   |                              | 11.11.2022 |            | OPPFYLT  | EØS_FORORDNINGEN |

      | 3456    | UNDER_18_ÅR      |                              | 09.04.2005 | 08.04.2023 | OPPFYLT  |                  |
      | 3456    | GIFT_PARTNERSKAP |                              | 09.04.2005 |            | OPPFYLT  |                  |
      | 3456    | LOVLIG_OPPHOLD   |                              | 11.11.2022 |            | OPPFYLT  | EØS_FORORDNINGEN |
      | 3456    | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 11.11.2022 |            | OPPFYLT  | EØS_FORORDNINGEN |
      | 3456    | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 11.11.2022 |            | OPPFYLT  | EØS_FORORDNINGEN |

      | 5678    | UNDER_18_ÅR      |                              | 06.04.2006 | 05.04.2024 | OPPFYLT  |                  |
      | 5678    | GIFT_PARTNERSKAP |                              | 06.04.2006 |            | OPPFYLT  |                  |
      | 5678    | LOVLIG_OPPHOLD   |                              | 11.11.2022 |            | OPPFYLT  | EØS_FORORDNINGEN |
      | 5678    | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 11.11.2022 |            | OPPFYLT  | EØS_FORORDNINGEN |
      | 5678    | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 11.11.2022 |            | OPPFYLT  | EØS_FORORDNINGEN |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3456    | 1            | 01.12.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3456    | 1            | 01.03.2023 | 31.03.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |

      | 5678    | 1            | 01.12.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5678    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 5678    | 1            | 01.07.2023 | 31.03.2024 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

      | 7890    | 1            | 01.12.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 7890    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 7890    | 1            | 01.07.2023 | 31.05.2028 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med kompetanser
      | AktørId          | Fra dato   | Til dato   | Resultat            | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 7890, 3456, 5678 | 01.12.2022 | 31.03.2023 | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | DK                             | DK                  |
      | 7890, 5678       | 01.04.2023 |            | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | DK                             | DK                  |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser           | Ugyldige begrunnelser |
      | 01.04.2023 | 30.06.2023 | UTBETALING         | EØS_FORORDNINGEN               | REDUKSJON_IKKE_ANSVAR_FOR_BARN |                       |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser | Eøsbegrunnelser                | Fritekster |
      | 01.04.2023 | 30.06.2023 |                      | REDUKSJON_IKKE_ANSVAR_FOR_BARN |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.04.2023 til 30.06.2023
      | Begrunnelse                    | Type | Barnas fødselsdatoer | Antall barn | Målform | Annen forelders aktivitetsland | Barnets bostedsland | Søkers aktivitetsland | Annen forelders aktivitet | Søkers aktivitet | Gjelder søker |
      | REDUKSJON_IKKE_ANSVAR_FOR_BARN | EØS  | 09.04.05             | 1           | NB      | Danmark                        | Danmark             | Norge                 | I_ARBEID                  | ARBEIDER         | Nei           |


  Scenario: Reduksjon før fylt 18

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 5678    | BARN       | 18.10.2009  |
      | 1            | 1234    | SØKER      | 30.03.1988  |
      | 1            | 3456    | BARN       | 15.04.2005  |

    Og dagens dato er 12.10.2023
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 3456    | GIFT_PARTNERSKAP                            |                  | 15.04.2005 |            | OPPFYLT  | Nei                  |
      | 3456    | UNDER_18_ÅR                                 |                  | 15.04.2005 | 14.04.2023 | OPPFYLT  | Nei                  |
      | 3456    | BOR_MED_SØKER                               |                  | 01.03.2022 | 27.06.2022 | OPPFYLT  | Nei                  |
      | 3456    | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 01.03.2022 |            | OPPFYLT  | Nei                  |

      | 1234    | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 01.03.2022 |            | OPPFYLT  | Nei                  |

      | 5678    | GIFT_PARTNERSKAP                            |                  | 18.10.2009 |            | OPPFYLT  | Nei                  |
      | 5678    | UNDER_18_ÅR                                 |                  | 18.10.2009 | 17.10.2027 | OPPFYLT  | Nei                  |
      | 5678    | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.03.2022 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3456    | 1            | 01.04.2022 | 30.06.2022 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5678    | 1            | 01.04.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5678    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 5678    | 1            | 01.07.2023 | 30.09.2027 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Når vedtaksperiodene genereres for behandling 1

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser   | Eøsbegrunnelser | Fritekster |
      | 01.07.2022 | 28.02.2023 | REDUKSJON_FLYTTET_BARN |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.07.2022 til 28.02.2023
      | Begrunnelse            | Barnas fødselsdatoer | Antall barn | Målform | Beløp | Gjelder søker | Måned og år begrunnelsen gjelder for |
      | REDUKSJON_FLYTTET_BARN | 15.04.05             | 1           | NB      | 0     | Nei           | juni 2022                            |

  Scenario: Reduksjon på ett barn - innvilgelse på det andre barnet - kun barnet som har flyttet vekk skal flettes inn i reduksjonstekst
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | ENDRET_UTBETALING   | SATSENDRING      | Ja                        | NASJONAL            |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | NYE_OPPLYSNINGER | Nei                       | NASJONAL            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 16.07.1975  |
      | 1            | 2       | BARN       | 09.06.2009  |
      | 1            | 3       | BARN       | 07.08.2011  |
      | 2            | 1       | SØKER      | 16.07.1975  |
      | 2            | 2       | BARN       | 09.06.2009  |
      | 2            | 3       | BARN       | 07.08.2011  |
    Og dagens dato er 19.10.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.04.2022 |            | OPPFYLT  | Nei                  |

      | 2       | GIFT_PARTNERSKAP              |                  | 09.06.2009 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                   |                  | 09.06.2009 | 08.06.2027 | OPPFYLT  | Nei                  |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 01.04.2022 |            | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED      | 01.04.2022 |            | OPPFYLT  | Nei                  |

      | 3       | UNDER_18_ÅR                   |                  | 07.08.2011 | 06.08.2029 | OPPFYLT  | Nei                  |
      | 3       | GIFT_PARTNERSKAP              |                  | 07.08.2011 |            | OPPFYLT  | Nei                  |
      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 01.04.2022 |            | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER                 | DELT_BOSTED      | 01.04.2022 |            | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.04.2022 |            | OPPFYLT      | Nei                  |

      | 2       | GIFT_PARTNERSKAP              |                  | 09.06.2009 |            | OPPFYLT      | Nei                  |
      | 2       | UNDER_18_ÅR                   |                  | 09.06.2009 | 08.06.2027 | OPPFYLT      | Nei                  |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.04.2022 |            | OPPFYLT      | Nei                  |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED      | 01.04.2022 | 02.01.2023 | OPPFYLT      | Nei                  |
      | 2       | BOR_MED_SØKER                 |                  | 03.01.2023 |            | OPPFYLT      | Nei                  |

      | 3       | GIFT_PARTNERSKAP              |                  | 07.08.2011 |            | OPPFYLT      | Nei                  |
      | 3       | UNDER_18_ÅR                   |                  | 07.08.2011 | 06.08.2029 | OPPFYLT      | Nei                  |
      | 3       | BOR_MED_SØKER                 | DELT_BOSTED      | 01.04.2022 | 02.01.2023 | OPPFYLT      | Nei                  |
      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.04.2022 |            | OPPFYLT      | Nei                  |
      | 3       | BOR_MED_SØKER                 |                  | 03.01.2023 |            | IKKE_OPPFYLT | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.05.2022 | 28.02.2023 | 527   | ORDINÆR_BARNETRYGD | 50      | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 542   | ORDINÆR_BARNETRYGD | 50      | 1083 |
      | 2       | 1            | 01.07.2023 | 31.05.2027 | 655   | ORDINÆR_BARNETRYGD | 50      | 1310 |
      | 3       | 1            | 01.05.2022 | 28.02.2023 | 527   | ORDINÆR_BARNETRYGD | 50      | 1054 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 542   | ORDINÆR_BARNETRYGD | 50      | 1083 |
      | 3       | 1            | 01.07.2023 | 31.07.2029 | 655   | ORDINÆR_BARNETRYGD | 50      | 1310 |

      | 2       | 2            | 01.05.2022 | 31.01.2023 | 527   | ORDINÆR_BARNETRYGD | 50      | 1054 |
      | 2       | 2            | 01.02.2023 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.07.2023 | 31.05.2027 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 2            | 01.05.2022 | 31.01.2023 | 527   | ORDINÆR_BARNETRYGD | 50      | 1054 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                                               | Ugyldige begrunnelser |
      | 01.02.2023 | 28.02.2023 | UTBETALING         |                                | INNVILGET_ENIGHET_OM_OPPHØR_AV_AVTALE_OM_DELT_BOSTED, REDUKSJON_AVTALE_FAST_BOSTED |                       |
      | 01.03.2023 | 30.06.2023 | UTBETALING         |                                | INNVILGET_SATSENDRING                                                              |                       |
      | 01.07.2023 | 31.05.2027 | UTBETALING         |                                | INNVILGET_SATSENDRING                                                              |                       |
      | 01.06.2027 |            | OPPHØR             |                                |                                                                                    |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                                                               | Eøsbegrunnelser | Fritekster |
      | 01.02.2023 | 28.02.2023 | INNVILGET_ENIGHET_OM_OPPHØR_AV_AVTALE_OM_DELT_BOSTED, REDUKSJON_AVTALE_FAST_BOSTED |                 |            |

    Så forvent følgende brevperioder for behandling 2
      | Brevperiodetype | Fra dato     | Til dato         | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |
      | UTBETALING      | februar 2023 | til februar 2023 | 1054  | 1                          | 09.06.09            | du                     |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.02.2023 til 28.02.2023
      | Begrunnelse                                          | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp |
      | REDUKSJON_AVTALE_FAST_BOSTED                         | STANDARD | Nei           | 07.08.11             | 1           | januar 2023                          | NB      | 0     |
      | INNVILGET_ENIGHET_OM_OPPHØR_AV_AVTALE_OM_DELT_BOSTED | STANDARD | Nei           | 09.06.09             | 1           | januar 2023                          | NB      | 1 054 |

  Scenario: Skal ikke flette inn barn som tidligere har opphørt i reduksjonstekst
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | ENDRET_UTBETALING   | NYE_OPPLYSNINGER | Nei                       | NASJONAL            |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | NYE_OPPLYSNINGER | Nei                       | NASJONAL            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 17.11.1986  |              |
      | 1            | 2       | BARN       | 01.09.2007  |              |
      | 1            | 3       | BARN       | 18.02.2011  | 08.03.2022   |
      | 1            | 4       | BARN       | 10.10.2023  |              |
      | 2            | 1       | SØKER      | 17.11.1986  |              |
      | 2            | 2       | BARN       | 01.09.2007  |              |
      | 2            | 3       | BARN       | 18.02.2011  | 08.03.2022   |
      | 2            | 4       | BARN       | 10.10.2023  |              |

    Og dagens dato er 29.02.2024
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD                          |                  | 01.02.2022 | 10.10.2023 | OPPFYLT  | Nei                  |                      |                  |
      | 1       | UTVIDET_BARNETRYGD                          |                  | 11.10.2023 | 01.02.2024 | OPPFYLT  | Nei                  |                      |                  |

      | 2       | GIFT_PARTNERSKAP                            |                  | 01.09.2007 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                                 |                  | 01.09.2007 | 31.08.2025 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER,LOVLIG_OPPHOLD |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | GIFT_PARTNERSKAP,UNDER_18_ÅR                |                  | 18.02.2011 | 08.03.2022 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.02.2022 | 08.03.2022 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 4       | GIFT_PARTNERSKAP                            |                  | 10.10.2023 |            | OPPFYLT  | Nei                  |                      |                  |
      | 4       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 10.10.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | UNDER_18_ÅR                                 |                  | 10.10.2023 | 09.10.2041 | OPPFYLT  | Nei                  |                      |                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD                          |                  | 01.02.2022 | 10.10.2023 | OPPFYLT  | Nei                  |                      |                  |
      | 1       | UTVIDET_BARNETRYGD                          |                  | 11.10.2023 | 01.02.2024 | OPPFYLT  | Nei                  |                      |                  |

      | 2       | GIFT_PARTNERSKAP                            |                  | 01.09.2007 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                                 |                  | 01.09.2007 | 31.08.2025 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                               |                  | 01.02.2022 | 01.02.2024 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR,GIFT_PARTNERSKAP                |                  | 18.02.2011 | 08.03.2022 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER |                  | 01.02.2022 | 08.03.2022 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 4       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 10.10.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | UNDER_18_ÅR                                 |                  | 10.10.2023 | 09.10.2041 | OPPFYLT  | Nei                  |                      |                  |
      | 4       | GIFT_PARTNERSKAP                            |                  | 10.10.2023 |            | OPPFYLT  | Nei                  |                      |                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.03.2022 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 1            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 1       | 1            | 01.07.2023 | 29.02.2024 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 1            | 01.03.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 1            | 01.01.2024 | 31.08.2025 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 1            | 01.03.2022 | 31.03.2022 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 4       | 1            | 01.11.2023 | 30.09.2029 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 1            | 01.10.2029 | 30.09.2041 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |

      | 1       | 2            | 01.03.2022 | 28.02.2023 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 2            | 01.03.2023 | 30.06.2023 | 2489  | UTVIDET_BARNETRYGD | 100     | 2489 |
      | 1       | 2            | 01.07.2023 | 29.02.2024 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 2            | 01.03.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 2            | 01.01.2024 | 29.02.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 2            | 01.03.2022 | 31.03.2022 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 4       | 2            | 01.11.2023 | 30.09.2029 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 4       | 2            | 01.10.2029 | 30.09.2041 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser   | Ugyldige begrunnelser |
      | 01.03.2024 | 30.09.2029 | UTBETALING         |                                | REDUKSJON_FLYTTET_BARN |                       |
      | 01.10.2029 | 30.09.2041 | UTBETALING         |                                |                        |                       |
      | 01.10.2041 |            | OPPHØR             |                                |                        |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser   | Eøsbegrunnelser | Fritekster |
      | 01.03.2024 | 30.09.2029 | REDUKSJON_FLYTTET_BARN |                 |            |

    Så forvent følgende brevperioder for behandling 2
      | Brevperiodetype | Fra dato  | Til dato           | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |
      | UTBETALING      | mars 2024 | til september 2029 | 1766  | 1                          | 10.10.23            | du                     |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.03.2024 til 30.09.2029
      | Begrunnelse            | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp |
      | REDUKSJON_FLYTTET_BARN | STANDARD | Nei           | 01.09.07             | 1           | februar 2024                         | NB      | 0     |