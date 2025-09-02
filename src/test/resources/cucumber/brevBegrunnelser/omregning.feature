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

  Scenario: Skal ha riktig begrunnelse for omregning 18 år EØS
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | Nei                       | EØS                 |
      | 2            | 1        | 1                   | FORTSATT_INNVILGET  | OMREGNING_18ÅR   | Ja                        | EØS                 |

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

    Og med kompetanser
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.01.2022 |          | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |
      | 2       | 01.01.2022 |          | NORGE_ER_SEKUNDÆRLAND | 2            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |

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
