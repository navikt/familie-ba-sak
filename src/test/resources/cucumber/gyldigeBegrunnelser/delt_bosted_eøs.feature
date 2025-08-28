# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for delt bosted i EØS-saker

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

  Scenario: Skal få begrunnelser for delt bosted skal deles i EØS-sak
    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat  | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | INNVILGET_OG_OPPHØRT | SØKNAD           | Nei                       | EØS                 |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 28.09.1977  |
      | 1            | 2       | BARN       | 18.06.2009  |
      | 1            | 3       | BARN       | 03.02.2018  |
    Og dagens dato er 11.12.2023
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår                     | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                                      | 01.08.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING         | 01.08.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | UNDER_18_ÅR      |                                      | 18.06.2009 | 17.06.2027 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP |                                      | 18.06.2009 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | LOVLIG_OPPHOLD   |                                      | 01.08.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER,DELT_BOSTED | 01.08.2023 | 01.11.2023 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS                       | 01.08.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 3       | UNDER_18_ÅR      |                                      | 03.02.2018 | 02.02.2036 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP |                                      | 03.02.2018 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | LOVLIG_OPPHOLD   |                                      | 01.08.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER,DELT_BOSTED | 01.08.2023 | 01.10.2023 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_EØS                       | 01.08.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.09.2023 | 30.11.2023 | 655   | ORDINÆR_BARNETRYGD | 50      | 1310 |
      | 3       | 1            | 01.09.2023 | 31.10.2023 | 883   | ORDINÆR_BARNETRYGD | 50      | 1766 |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat            | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 3, 2    | 01.09.2023 | 31.10.2023 | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | INAKTIV                   | NO                    | DK                             | DK                  |
      | 2       | 01.11.2023 | 30.11.2023 | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | INAKTIV                   | NO                    | DK                             | DK                  |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                             | Ugyldige begrunnelser |
      | 01.09.2023 | 31.10.2023 | UTBETALING         | EØS_FORORDNINGEN               | INNVILGET_PRIMÆRLAND_STANDARD                                    |                       |
      | 01.11.2023 | 30.11.2023 | UTBETALING         | EØS_FORORDNINGEN               | REDUKSJON_DELT_BOSTED_BEGGE_FORELDRE_IKKE_OMFATTET_NORSK_LOVVALG |                       |
      | 01.12.2023 |            | OPPHØR             | EØS_FORORDNINGEN               | OPPHØR_DELT_BOSTED_BEGGE_FORELDRE_IKKE_OMFATTET_NORSK_LOVGIVNING |                       |


  Scenario: Skal få begrunnelser for delt bosted skal ikke deles i EØS-sak
    Gitt følgende behandlinger
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 11.01.1970  |
      | 1            | 2       | BARN       | 13.04.2020  |

    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Utdypende vilkår                                     | Fra dato   | Til dato   | Resultat | Vurderes etter   |
      | 1       | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   |                                                      | 11.01.1970 |            | Oppfylt  |                  |
      | 2       | UNDER_18_ÅR                                      |                                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |                  |
      | 2       | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD |                                                      | 13.04.2020 |            | Oppfylt  |                  |
      | 2       | BOR_MED_SØKER                                    | BARN_BOR_I_EØS_MED_SØKER,DELT_BOSTED_SKAL_IKKE_DELES | 13.04.2020 | 10.03.2021 | Oppfylt  | EØS_FORORDNINGEN |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 2       | 01.05.2020 | 31.03.2021 | 1354  | 1            |


    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat            | BehandlingId | Annen forelders aktivitet | Barnets bostedsland |
      | 2       | 01.05.2020 | 31.03.2021 | NORGE_ER_PRIMÆRLAND | 1            | IKKE_AKTUELT              | NO                  |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                    | Ugyldige begrunnelser |
      | 01.05.2020 | 31.03.2021 | UTBETALING         | EØS_FORORDNINGEN               | INNVILGET_TILLEGGSTEKST_FULL_BARNETRYGD_HAR_AVTALE_DELT |                       |
      | 01.04.2021 |            | OPPHØR             |                                |                                                         |                       |
