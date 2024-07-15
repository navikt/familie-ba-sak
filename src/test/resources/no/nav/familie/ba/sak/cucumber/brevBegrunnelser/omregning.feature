# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser ved omregningsbehandlinger

  Bakgrunn:

  Scenario: Skal ha riktig begrunnelse for omregning 18 år
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | Nei                       |
      | 2            | 1        | 1                   | FORTSATT_INNVILGET  | OMREGNING_18ÅR   | Ja                        |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 12.11.1996  |
      | 1            | 2       | BARN       | 11.10.2005  |
      | 1            | 3       | BARN       | 11.10.2021  |
      | 2            | 1       | SØKER      | 12.11.1996  |
      | 2            | 2       | BARN       | 11.10.2005  |
      | 2            | 3       | BARN       | 11.10.2021  |

    Og dagens dato er 11.10.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET                                |                  | 11.08.2023 |            | OPPFYLT  | Nei                  |

      | 2       | UNDER_18_ÅR                                                  |                  | 11.10.2005 | 10.10.2023 | OPPFYLT  | Nei                  |
      | 2       | GIFT_PARTNERSKAP,BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 11.08.2023 |            | OPPFYLT  | Nei                  |

      | 3       | UNDER_18_ÅR                                                  |                  | 11.10.2021 | 10.10.2039 | OPPFYLT  | Nei                  |
      | 3       | GIFT_PARTNERSKAP,BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 11.08.2023 |            | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD                                |                  | 11.08.2023 |            | OPPFYLT  | Nei                  |

      | 2       | UNDER_18_ÅR                                                  |                  | 11.10.2005 | 10.10.2023 | OPPFYLT  | Nei                  |
      | 2       | GIFT_PARTNERSKAP,BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 11.08.2023 |            | OPPFYLT  | Nei                  |

      | 3       | UNDER_18_ÅR                                                  |                  | 11.10.2021 | 10.10.2039 | OPPFYLT  | Nei                  |
      | 3       | GIFT_PARTNERSKAP,BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 11.08.2023 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.09.2023 | 30.09.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 1            | 01.09.2023 | 30.09.2027 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.10.2027 | 30.09.2039 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2       | 2            | 01.09.2023 | 30.09.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 2            | 01.09.2023 | 30.09.2027 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 2            | 01.10.2027 | 30.09.2039 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Gyldige begrunnelser             | Ugyldige begrunnelser |
      | 01.10.2023 | 30.09.2027 | Utbetaling         |           | REDUKSJON_UNDER_18_ÅR_AUTOVEDTAK |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser             | Eøsbegrunnelser | Fritekster |
      | 01.10.2023 | 30.09.2027 | REDUKSJON_UNDER_18_ÅR_AUTOVEDTAK |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.10.2023 til 30.09.2027
      | Begrunnelse                      | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | REDUKSJON_UNDER_18_ÅR_AUTOVEDTAK | Nei           | 11.10.05             | 1           | september 2023                       | NB      | 0     |                  | SØKER_HAR_IKKE_RETT     |

  Scenario: Skal ha riktig begrunnelse for omregning 6 år
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | Nei                       |
      | 2            | 1        | 1                   | FORTSATT_INNVILGET  | OMREGNING_6ÅR    | Ja                        |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 12.11.1996  |
      | 1            | 2       | BARN       | 11.10.2005  |
      | 1            | 3       | BARN       | 11.10.2017  |
      | 2            | 1       | SØKER      | 12.11.1996  |
      | 2            | 2       | BARN       | 11.10.2005  |
      | 2            | 3       | BARN       | 11.10.2017  |
    Og dagens dato er 11.10.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 11.08.2023 |            | OPPFYLT  | Nei                  |

      | 2       | UNDER_18_ÅR                                 |                  | 11.10.2005 | 10.10.2023 | OPPFYLT  | Nei                  |
      | 2       | GIFT_PARTNERSKAP                            |                  | 11.10.2005 |            | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 11.08.2023 |            | OPPFYLT  | Nei                  |


      | 3       | UNDER_18_ÅR                                 |                  | 11.10.2017 | 10.10.2035 | OPPFYLT  | Nei                  |
      | 3       | GIFT_PARTNERSKAP                            |                  | 11.10.2017 |            | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 11.08.2023 |            | OPPFYLT  | Nei                  |



    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 11.08.2023 |            | OPPFYLT  | Nei                  |

      | 2       | UNDER_18_ÅR                                 |                  | 11.10.2005 | 10.10.2023 | OPPFYLT  | Nei                  |
      | 2       | GIFT_PARTNERSKAP                            |                  | 11.10.2005 |            | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 11.08.2023 |            | OPPFYLT  | Nei                  |


      | 3       | UNDER_18_ÅR                                 |                  | 11.10.2017 | 10.10.2035 | OPPFYLT  | Nei                  |
      | 3       | GIFT_PARTNERSKAP                            |                  | 11.10.2017 |            | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 11.08.2023 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.09.2023 | 30.09.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 1            | 01.09.2023 | 30.09.2023 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.10.2023 | 30.09.2035 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

      | 2       | 2            | 01.09.2023 | 30.09.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 2            | 01.09.2023 | 30.09.2023 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 2            | 01.10.2023 | 30.09.2035 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Gyldige begrunnelser            | Ugyldige begrunnelser |
      | 01.10.2023 | 30.09.2035 | UTBETALING         |           | REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser            | Eøsbegrunnelser | Fritekster |
      | 01.10.2023 | 30.09.2035 | REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.10.2023 til 30.09.2035
      | Begrunnelse                     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK | Nei           | 11.10.17             | 1           | september 2023                       | NB      | 1 310 |                  | SØKER_HAR_IKKE_RETT     |


  Scenario: Skal ha riktig begrunnelse for omregning 6 år EØS

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | ENDRET_UTBETALING   | SATSENDRING      | Ja                        | EØS                 |
      | 2            | 1        | 1                   | FORTSATT_INNVILGET  | OMREGNING_6ÅR    | Ja                        | EØS                 |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 17.10.1985  |
      | 1            | 2       | BARN       | 24.10.2017  |
      | 2            | 1       | SØKER      | 17.10.1985  |
      | 2            | 2       | BARN       | 24.10.2017  |


    Og dagens dato er 24.10.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 01.12.2021 |            | OPPFYLT  | Nei                  |                      |
      | 1       | LOVLIG_OPPHOLD   |                              | 01.12.2021 |            | OPPFYLT  | Nei                  |                      |

      | 2       | UNDER_18_ÅR      |                              | 24.10.2017 | 23.10.2035 | OPPFYLT  | Nei                  |                      |
      | 2       | GIFT_PARTNERSKAP |                              | 24.10.2017 |            | OPPFYLT  | Nei                  |                      |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 01.12.2021 |            | OPPFYLT  | Nei                  |                      |
      | 2       | LOVLIG_OPPHOLD   |                              | 01.12.2021 |            | OPPFYLT  | Nei                  |                      |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 01.12.2021 |            | OPPFYLT  | Nei                  |                      |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 01.12.2021 |            | OPPFYLT  | Nei                  |                      |
      | 1       | LOVLIG_OPPHOLD   |                              | 01.12.2021 |            | OPPFYLT  | Nei                  |                      |

      | 2       | UNDER_18_ÅR      |                              | 24.10.2017 | 23.10.2035 | OPPFYLT  | Nei                  |                      |
      | 2       | GIFT_PARTNERSKAP |                              | 24.10.2017 |            | OPPFYLT  | Nei                  |                      |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 01.12.2021 |            | OPPFYLT  | Nei                  |                      |
      | 2       | LOVLIG_OPPHOLD   |                              | 01.12.2021 |            | OPPFYLT  | Nei                  |                      |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 01.12.2021 |            | OPPFYLT  | Nei                  |                      |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.01.2022 | 28.02.2023 | 553   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 600   | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 2       | 1            | 01.07.2023 | 30.09.2023 | 643   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.10.2023 | 30.09.2035 | 187   | ORDINÆR_BARNETRYGD | 100     | 1310 |

      | 2       | 2            | 01.01.2022 | 28.02.2023 | 553   | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 600   | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 2       | 2            | 01.07.2023 | 30.09.2023 | 643   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 2            | 01.10.2023 | 30.09.2035 | 187   | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.01.2022 |          | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |
      | 2       | 01.01.2022 |          | NORGE_ER_SEKUNDÆRLAND | 2            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser            | Ugyldige begrunnelser |
      | 01.10.2023 | 30.09.2035 | UTBETALING         |                                | REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK |                       |


    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser            | Eøsbegrunnelser | Fritekster |
      | 01.10.2023 | 30.09.2035 | REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK |                 |            |


    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.10.2023 til 30.09.2035
      | Begrunnelse                     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK | Nei           | 24.10.17             | 1           | september 2023                       | NB      | 187   |                  | SØKER_HAR_IKKE_RETT     |