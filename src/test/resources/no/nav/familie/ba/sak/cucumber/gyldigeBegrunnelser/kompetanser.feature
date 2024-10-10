# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for kompetanser

  Bakgrunn:
    Gitt følgende behandlinger
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 13.04.2020  |

  Scenario: Skal gi innvilget primærland begrunnelse basert på kompetanse
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat | Vurderes etter   |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |                  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |                  |
      | 3456    | BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  | EØS_FORORDNINGEN |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 30.04.2021 | 1054  | 1            |
      | 3456    | 01.05.2021 | 31.03.2038 | 1354  | 1            |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Annen forelders aktivitet | Barnets bostedsland |
      | 3456    | 01.05.2020 | 30.04.2021 | NORGE_ER_PRIMÆRLAND   | 1            | IKKE_AKTUELT              | NO                  |
      | 3456    | 01.05.2021 | 31.03.2038 | NORGE_ER_SEKUNDÆRLAND | 1            | I_ARBEID                  | PL                  |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                               | Ugyldige begrunnelser                                 |
      | 01.05.2020 | 30.04.2021 | Utbetaling         | EØS_FORORDNINGEN               | INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_BOSATT_I_NORGE | INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_JOBBER_I_NORGE    |
      | 01.05.2021 | 31.03.2038 | Utbetaling         | EØS_FORORDNINGEN               | INNVILGET_SEKUNDÆRLAND_STANDARD                    | INNVILGET_SEKUNDÆRLAND_TO_ARBEIDSLAND_NORGE_UTBETALER |
      | 01.04.2038 |            | Opphør             |                                |                                                    |                                                       |

  Scenario: Ikke vis kompetansebegrunnelser dersom kompetansen ikke endrer seg
    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId |
      | 1            | 1        |                     |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 30.09.1984  |
      | 1            | 2       | BARN       | 02.10.2017  |

    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                          | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD                  |                              | 30.09.1984 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                  | OMFATTET_AV_NORSK_LOVGIVNING | 15.07.2023 |            | OPPFYLT  | Nei                  |

      | 2       | BOR_MED_SØKER                   | BARN_BOR_I_EØS_MED_SØKER     | 02.10.2017 |            | OPPFYLT  | Nei                  |
      | 2       | LOVLIG_OPPHOLD,GIFT_PARTNERSKAP |                              | 02.10.2017 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                     |                              | 02.10.2017 | 01.10.2035 | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET                  | BARN_BOR_I_NORGE             | 02.10.2017 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 2       | 1            | 01.08.2023 | 31.08.2023 | 1766  | ORDINÆR_BARNETRYGD | 100     |
      | 2       | 1            | 01.09.2023 | 30.09.2023 | 1400  | ORDINÆR_BARNETRYGD | 100     |
      | 2       | 1            | 01.10.2023 | 30.09.2035 | 1310  | ORDINÆR_BARNETRYGD | 100     |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Annen forelders aktivitet | Barnets bostedsland |
      | 2       | 01.08.2023 | 31.08.2023 | NORGE_ER_PRIMÆRLAND   | 1            | INAKTIV                   | NO                  |
      | 2       | 01.09.2023 |            | NORGE_ER_SEKUNDÆRLAND | 1            | I_ARBEID                  | GB                  |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser | Regelverk Ugyldige begrunnelser | Ugyldige begrunnelser          |
      | 01.10.2023 | 30.09.2035 | UTBETALING         |                                | REDUKSJON_UNDER_6_ÅR | EØS_FORORDNINGEN                | REDUKSJON_IKKE_ANSVAR_FOR_BARN |

  Scenario: Skal gi riktig begrunnelse ved opphør av EØS-sak
    Gitt følgende behandlinger
      | BehandlingId | FagsakId  | ForrigeBehandlingId |
      | 100173207    | 200055651 |                     |

    Og følgende persongrunnlag
      | BehandlingId | AktørId       | Persontype | Fødselsdato |
      | 100173207    | 2005858678161 | BARN       | 02.02.2015  |
      | 100173207    | 2305793738737 | SØKER      | 12.11.1984  |

    Og lag personresultater for behandling 100173207

    Og legg til nye vilkårresultater for behandling 100173207
      | AktørId       | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Vurderes etter   |
      | 2005858678161 | GIFT_PARTNERSKAP |                              | 02.02.2015 |            | OPPFYLT  | Nei                  |                  |
      | 2005858678161 | LOVLIG_OPPHOLD   |                              | 02.02.2015 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 2005858678161 | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 02.02.2015 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 2005858678161 | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 02.02.2015 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 2005858678161 | UNDER_18_ÅR      |                              | 02.02.2015 | 01.02.2033 | OPPFYLT  | Nei                  |                  |

      | 2305793738737 | LOVLIG_OPPHOLD   |                              | 12.11.1984 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 2305793738737 | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 15.03.2023 | 15.08.2023 | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

    Og med andeler tilkjent ytelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 2005858678161 | 100173207    | 01.04.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     |
      | 2005858678161 | 100173207    | 01.07.2023 | 31.08.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     |

    Og med kompetanser
      | AktørId       | Fra dato   | Til dato   | Resultat            | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2005858678161 | 01.04.2023 | 31.08.2023 | NORGE_ER_PRIMÆRLAND | 100173207    | ARBEIDER         | MOTTAR_PENSJON            | NO                    | BE                             | NO                  |

    Når vedtaksperiodene genereres for behandling 100173207

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser               | Ugyldige begrunnelser |
      | 01.04.2023 | 30.06.2023 | UTBETALING         |                                |                                    |                       |
      | 01.07.2023 | 31.08.2023 | UTBETALING         |                                |                                    |                       |
      | 01.09.2023 |            | OPPHØR             | EØS_FORORDNINGEN               | OPPHØR_IKKE_STATSBORGER_I_EØS_LAND |                       |

  Scenario: Skal begrunne endring i kompetanse når det ikke er noen endringer i resten av behandlingen
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId |
      | 1            | 1        |                     |
      | 2            | 1        | 1                   |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | BARN       | 02.02.2015  |
      | 1            | 5678    | SØKER      | 10.05.1985  |
      | 2            | 1234    | BARN       | 02.02.2015  |
      | 2            | 5678    | SØKER      | 10.05.1985  |

    Og dagens dato er 12.09.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Vurderes etter   |
      | 5678    | LOVLIG_OPPHOLD   |                              | 10.05.1985 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 5678    | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 15.02.2021 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

      | 1234    | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 02.02.2015 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 1234    | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 02.02.2015 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 1234    | UNDER_18_ÅR      |                              | 02.02.2015 | 01.02.2033 | OPPFYLT  | Nei                  |                  |
      | 1234    | LOVLIG_OPPHOLD   |                              | 02.02.2015 |            | OPPFYLT  | Nei                  |                  |
      | 1234    | GIFT_PARTNERSKAP |                              | 02.02.2015 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Vurderes etter   |
      | 1234    | GIFT_PARTNERSKAP |                              | 02.02.2015 |            | OPPFYLT  | Nei                  |                  |
      | 1234    | LOVLIG_OPPHOLD   |                              | 02.02.2015 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 1234    | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 02.02.2015 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 1234    | UNDER_18_ÅR      |                              | 02.02.2015 | 01.02.2033 | OPPFYLT  | Nei                  |                  |
      | 1234    | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 02.02.2015 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

      | 5678    | LOVLIG_OPPHOLD   |                              | 10.05.1985 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 5678    | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 15.02.2021 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 1234    | 1            | 01.03.2021 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     |
      | 1234    | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     |
      | 1234    | 1            | 01.07.2023 | 31.01.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     |
      | 1234    | 2            | 01.03.2021 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     |
      | 1234    | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     |
      | 1234    | 2            | 01.07.2023 | 31.01.2033 | 1310  | ORDINÆR_BARNETRYGD | 100     |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat            | BehandlingId | Søkers aktivitet  | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 1234    | 01.03.2021 |            | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER          | INAKTIV                   | NO                    | BE                             | NO                  |
      | 1234    | 01.03.2021 | 30.04.2023 | NORGE_ER_PRIMÆRLAND | 2            | ARBEIDER          | INAKTIV                   | NO                    | BE                             | NO                  |
      | 1234    | 01.05.2023 |            | NORGE_ER_PRIMÆRLAND | 2            | MOTTAR_UFØRETRYGD | INAKTIV                   | NO                    | EE                             | NO                  |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                    | Ugyldige begrunnelser                                                                          |
      | 01.05.2023 | 30.06.2023 | UTBETALING         | EØS_FORORDNINGEN               | INNVILGET_PRIMÆRLAND_BARNET_BOR_I_NORGE | REDUKSJON_BARN_DØD_EØS, REDUKSJON_IKKE_ANSVAR_FOR_BARN, FORTSATT_INNVILGET_PRIMÆRLAND_STANDARD |
      | 01.07.2023 | 31.01.2033 | UTBETALING         |                                |                                         |                                                                                                |
      | 01.02.2033 |            | OPPHØR             |                                |                                         |                                                                                                |

  Scenario: Skal kunne begrunne valutajustering når det er reduksjon i samme måned
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | ENDRET_UTBETALING   | SATSENDRING      | Ja                        | EØS                 |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | NYE_OPPLYSNINGER | Nei                       | EØS                 |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 16.01.1984  |
      | 1            | 2       | BARN       | 28.02.2009  |
      | 1            | 3       | BARN       | 27.08.2011  |
      | 1            | 4       | BARN       | 20.05.2017  |
      | 2            | 1       | SØKER      | 16.01.1984  |
      | 2            | 2       | BARN       | 28.02.2009  |
      | 2            | 3       | BARN       | 27.08.2011  |
      | 2            | 4       | BARN       | 20.05.2017  |
    Og dagens dato er 03.11.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår                    | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                                     | 01.04.2022 |            | OPPFYLT  | Nei                  |                  |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING_UTLAND | 01.04.2022 |            | OPPFYLT  | Nei                  |                  |

      | 2       | GIFT_PARTNERSKAP |                                     | 28.02.2009 |            | OPPFYLT  | Nei                  |                  |
      | 2       | UNDER_18_ÅR      |                                     | 28.02.2009 | 27.02.2027 | OPPFYLT  | Nei                  |                  |
      | 2       | LOVLIG_OPPHOLD   |                                     | 01.04.2022 |            | OPPFYLT  | Nei                  |                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_STORBRITANNIA_MED_SØKER  | 01.04.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_STORBRITANNIA            | 01.04.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

      | 3       | GIFT_PARTNERSKAP |                                     | 27.08.2011 |            | OPPFYLT  | Nei                  |                  |
      | 3       | UNDER_18_ÅR      |                                     | 27.08.2011 | 26.08.2029 | OPPFYLT  | Nei                  |                  |
      | 3       | BOR_MED_SØKER    | BARN_BOR_I_STORBRITANNIA_MED_SØKER  | 01.04.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 3       | LOVLIG_OPPHOLD   |                                     | 01.04.2022 |            | OPPFYLT  | Nei                  |                  |
      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_EØS                      | 01.04.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

      | 4       | GIFT_PARTNERSKAP |                                     | 20.05.2017 |            | OPPFYLT  | Nei                  |                  |
      | 4       | UNDER_18_ÅR      |                                     | 20.05.2017 | 19.05.2035 | OPPFYLT  | Nei                  |                  |
      | 4       | BOR_MED_SØKER    | BARN_BOR_I_STORBRITANNIA_MED_SØKER  | 01.04.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 4       | BOSATT_I_RIKET   | BARN_BOR_I_STORBRITANNIA            | 01.04.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 4       | LOVLIG_OPPHOLD   |                                     | 01.04.2022 |            | OPPFYLT  | Nei                  |                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår                    | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Vurderes etter   |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING_UTLAND | 01.04.2022 |            | OPPFYLT  | Nei                  |                  |
      | 1       | LOVLIG_OPPHOLD   |                                     | 01.04.2022 |            | OPPFYLT  | Nei                  |                  |

      | 2       | UNDER_18_ÅR      |                                     | 28.02.2009 | 27.02.2027 | OPPFYLT  | Nei                  |                  |
      | 2       | GIFT_PARTNERSKAP |                                     | 28.02.2009 |            | OPPFYLT  | Nei                  |                  |
      | 2       | LOVLIG_OPPHOLD   |                                     | 01.04.2022 |            | OPPFYLT  | Nei                  |                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_STORBRITANNIA_MED_SØKER  | 01.04.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_STORBRITANNIA            | 01.04.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

      | 3       | UNDER_18_ÅR      |                                     | 27.08.2011 | 26.08.2029 | OPPFYLT  | Nei                  |                  |
      | 3       | GIFT_PARTNERSKAP |                                     | 27.08.2011 |            | OPPFYLT  | Nei                  |                  |
      | 3       | LOVLIG_OPPHOLD   |                                     | 01.04.2022 |            | OPPFYLT  | Nei                  |                  |
      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_EØS                      | 01.04.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 3       | BOR_MED_SØKER    | BARN_BOR_I_STORBRITANNIA_MED_SØKER  | 01.04.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

      | 4       | UNDER_18_ÅR      |                                     | 20.05.2017 | 19.05.2035 | OPPFYLT  | Nei                  |                  |
      | 4       | GIFT_PARTNERSKAP |                                     | 20.05.2017 |            | OPPFYLT  | Nei                  |                  |
      | 4       | LOVLIG_OPPHOLD   |                                     | 01.04.2022 |            | OPPFYLT  | Nei                  |                  |
      | 4       | BOR_MED_SØKER    | BARN_BOR_I_STORBRITANNIA_MED_SØKER  | 01.04.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 4       | BOSATT_I_RIKET   | BARN_BOR_I_STORBRITANNIA            | 01.04.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.05.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 31.01.2027 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 1            | 01.05.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 1            | 01.07.2023 | 31.07.2029 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 4       | 1            | 01.05.2022 | 28.02.2023 | 557   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 4       | 1            | 01.03.2023 | 30.04.2023 | 604   | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 4       | 1            | 01.05.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 4       | 1            | 01.07.2023 | 30.04.2035 | 191   | ORDINÆR_BARNETRYGD | 100     | 1310 |

      | 2       | 2            | 01.05.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.07.2023 | 31.01.2027 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 2            | 01.05.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 2            | 01.07.2023 | 31.07.2029 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 4       | 2            | 01.05.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 4       | 2            | 01.03.2023 | 30.04.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 4       | 2            | 01.05.2023 | 30.06.2023 | 144   | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 4       | 2            | 01.07.2023 | 30.04.2035 | 371   | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet  | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 3, 2    | 01.05.2022 |          | NORGE_ER_PRIMÆRLAND   | 1            | MOTTAR_UFØRETRYGD | I_ARBEID                  | NO                    | NO                             | GB                  |
      | 4       | 01.05.2022 |          | NORGE_ER_SEKUNDÆRLAND | 1            | MOTTAR_UFØRETRYGD | I_ARBEID                  | NO                    | GB                             | GB                  |
      | 3, 2    | 01.05.2022 |          | NORGE_ER_PRIMÆRLAND   | 2            | MOTTAR_UFØRETRYGD | I_ARBEID                  | NO                    | NO                             | GB                  |
      | 4       | 01.05.2022 |          | NORGE_ER_SEKUNDÆRLAND | 2            | MOTTAR_UFØRETRYGD | I_ARBEID                  | NO                    | GB                             | GB                  |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                    | Ugyldige begrunnelser |
      | 01.05.2023 | 30.06.2023 | UTBETALING         |                                | REDUKSJON_UNDER_6_ÅR                    |                       |
      | 01.05.2023 | 30.06.2023 | UTBETALING         | EØS_FORORDNINGEN               | REDUKSJON_TILLEGGSTEKST_VALUTAJUSTERING |                       |

  Scenario: Skal dele opp begrunnelse etter antall kompetanser og skal kun dra med riktige barn per kompetanse
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | ENDRET_UTBETALING   | SATSENDRING      | Ja                        | EØS                 |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | NYE_OPPLYSNINGER | Nei                       | EØS                 |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 17.12.1976  |
      | 1            | 2       | BARN       | 10.08.2009  |
      | 1            | 3       | BARN       | 07.05.2014  |
      | 2            | 1       | SØKER      | 17.12.1976  |
      | 2            | 2       | BARN       | 10.08.2009  |
      | 2            | 3       | BARN       | 07.05.2014  |

    Og dagens dato er 07.11.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                              | 09.08.2021 |            | OPPFYLT  | Nei                  |                  |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 15.12.2021 | 15.03.2022 | OPPFYLT  | Nei                  |                  |

      | 2       | UNDER_18_ÅR      |                              | 10.08.2009 | 09.08.2027 | OPPFYLT  | Nei                  |                  |
      | 2       | GIFT_PARTNERSKAP |                              | 10.08.2009 |            | OPPFYLT  | Nei                  |                  |
      | 2       | LOVLIG_OPPHOLD   |                              | 09.08.2021 |            | OPPFYLT  | Nei                  |                  |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 09.08.2021 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_NORGE_MED_SØKER   | 09.08.2021 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

      | 3       | UNDER_18_ÅR      |                              | 07.05.2014 | 06.05.2032 | OPPFYLT  | Nei                  |                  |
      | 3       | GIFT_PARTNERSKAP |                              | 07.05.2014 |            | OPPFYLT  | Nei                  |                  |
      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 09.08.2021 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 3       | BOR_MED_SØKER    | BARN_BOR_I_NORGE_MED_SØKER   | 09.08.2021 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 3       | LOVLIG_OPPHOLD   |                              | 09.08.2021 |            | OPPFYLT  | Nei                  |                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                              | 09.08.2021 |            | OPPFYLT  | Nei                  |                  |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 15.12.2021 | 15.03.2022 | OPPFYLT  | Nei                  |                  |

      | 2       | UNDER_18_ÅR      |                              | 10.08.2009 | 09.08.2027 | OPPFYLT  | Nei                  |                  |
      | 2       | GIFT_PARTNERSKAP |                              | 10.08.2009 |            | OPPFYLT  | Nei                  |                  |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 09.08.2021 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 09.08.2021 |            | OPPFYLT  | Nei                  |                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_NORGE_MED_SØKER   | 09.08.2021 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

      | 3       | GIFT_PARTNERSKAP |                              | 07.05.2014 |            | OPPFYLT  | Nei                  |                  |
      | 3       | UNDER_18_ÅR      |                              | 07.05.2014 | 06.05.2032 | OPPFYLT  | Nei                  |                  |
      | 3       | BOR_MED_SØKER    | BARN_BOR_I_NORGE_MED_SØKER   | 09.08.2021 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 3       | LOVLIG_OPPHOLD   |                              | 09.08.2021 |            | OPPFYLT  | Nei                  |                  |
      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 09.08.2021 |            | OPPFYLT  | Nei                  |                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.01.2022 | 31.03.2022 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.01.2022 | 31.03.2022 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.01.2022 | 31.03.2022 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 2            | 01.01.2022 | 31.03.2022 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat            | BehandlingId | Søkers aktivitet                     | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.01.2022 | 31.03.2022 | NORGE_ER_PRIMÆRLAND | 2            | MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN | INAKTIV                   | NO                    | PL                             | NO                  |
      | 3       | 01.01.2022 | 31.03.2022 | NORGE_ER_PRIMÆRLAND | 2            | MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN | I_ARBEID                  | NO                    | PL                             | NO                  |

    Når vedtaksperiodene genereres for behandling 2

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser | Eøsbegrunnelser               | Fritekster |
      | 01.01.2022 | 31.03.2022 |                      | INNVILGET_PRIMÆRLAND_STANDARD |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.01.2022 til 31.03.2022
      | Begrunnelse                   | Type | Barnas fødselsdatoer | Antall barn | Målform | Søkers aktivitet                     | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | INNVILGET_PRIMÆRLAND_STANDARD | EØS  | 10.08.09             | 1           | NB      | MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN | INAKTIV                   | Norge                 | Polen                          | Norge               |
      | INNVILGET_PRIMÆRLAND_STANDARD | EØS  | 07.05.14             | 1           | NB      | MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN | I_ARBEID                  | Norge                 | Polen                          | Norge               |

  Scenario: Skal kunne begrunne nullutbetaling når det er reduksjon i samme måned
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | ENDRET_UTBETALING   | SATSENDRING      | Ja                        | EØS                 |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | NYE_OPPLYSNINGER | Nei                       | EØS                 |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 16.01.1984  |
      | 1            | 2       | BARN       | 20.05.2017  |
      | 2            | 1       | SØKER      | 16.01.1984  |
      | 2            | 2       | BARN       | 20.05.2017  |

    Og dagens dato er 03.11.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår                    | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                                     | 01.04.2022 |            | OPPFYLT  | Nei                  |                  |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING_UTLAND | 01.04.2022 |            | OPPFYLT  | Nei                  |                  |

      | 2       | GIFT_PARTNERSKAP |                                     | 20.05.2017 |            | OPPFYLT  | Nei                  |                  |
      | 2       | UNDER_18_ÅR      |                                     | 20.05.2017 | 19.05.2035 | OPPFYLT  | Nei                  |                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_STORBRITANNIA_MED_SØKER  | 01.04.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_STORBRITANNIA            | 01.04.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                                     | 01.04.2022 |            | OPPFYLT  | Nei                  |                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår                    | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Vurderes etter   |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING_UTLAND | 01.04.2022 |            | OPPFYLT  | Nei                  |                  |
      | 1       | LOVLIG_OPPHOLD   |                                     | 01.04.2022 |            | OPPFYLT  | Nei                  |                  |

      | 2       | UNDER_18_ÅR      |                                     | 20.05.2017 | 19.05.2035 | OPPFYLT  | Nei                  |                  |
      | 2       | GIFT_PARTNERSKAP |                                     | 20.05.2017 |            | OPPFYLT  | Nei                  |                  |
      | 2       | LOVLIG_OPPHOLD   |                                     | 01.04.2022 |            | OPPFYLT  | Nei                  |                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_STORBRITANNIA_MED_SØKER  | 01.04.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_STORBRITANNIA            | 01.04.2022 |            | OPPFYLT  | Nei                  | EØS_FORORDNINGEN |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.05.2022 | 28.02.2023 | 557   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 1            | 01.03.2023 | 30.04.2023 | 604   | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 2       | 1            | 01.05.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 30.04.2035 | 191   | ORDINÆR_BARNETRYGD | 100     | 1310 |

      | 2       | 2            | 01.05.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 2            | 01.03.2023 | 30.04.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 2       | 2            | 01.05.2023 | 30.06.2023 | 144   | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.07.2023 | 30.04.2035 | 371   | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet  | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.05.2022 |          | NORGE_ER_SEKUNDÆRLAND | 1            | MOTTAR_UFØRETRYGD | I_ARBEID                  | NO                    | GB                             | GB                  |
      | 2       | 01.05.2022 |          | NORGE_ER_SEKUNDÆRLAND | 2            | MOTTAR_UFØRETRYGD | I_ARBEID                  | NO                    | GB                             | GB                  |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                   | Ugyldige begrunnelser |
      | 01.05.2023 | 30.06.2023 | UTBETALING         |                                | REDUKSJON_UNDER_6_ÅR                   |                       |
      | 01.05.2023 | 30.06.2023 | UTBETALING         | EØS_FORORDNINGEN               | REDUKSJON_TILLEGGSTEKST_NULLUTBETALING |                       |

