# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser ved opphør når et barn er født eller flyttet inn hos søker etter opphørstidspunktet

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | Nei                       | NASJONAL            |
      | 2            | 1        | 1                   | OPPHØRT             | NYE_OPPLYSNINGER | Nei                       | NASJONAL            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 15.01.1988  |
      | 1            | 2       | BARN       | 06.03.2014  |
      | 1            | 3       | BARN       | 21.12.2016  |
      | 1            | 4       | BARN       | 15.03.2021  |
      | 2            | 1       | SØKER      | 15.01.1988  |
      | 2            | 2       | BARN       | 06.03.2014  |
      | 2            | 3       | BARN       | 21.12.2016  |
      | 2            | 4       | BARN       | 15.03.2021  |

    Og dagens dato er 05.10.2026
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD                                |                  | 01.01.2019 |            | OPPFYLT  | Nei                  |

      | 2       | GIFT_PARTNERSKAP                                             |                  | 06.03.2014 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                                  |                  | 06.03.2014 | 05.03.2032 | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD                  |                  | 01.01.2019 |            | OPPFYLT  | Nei                  |

      | 3       | GIFT_PARTNERSKAP                                             |                  | 21.12.2016 |            | OPPFYLT  | Nei                  |
      | 3       | UNDER_18_ÅR                                                  |                  | 21.12.2016 | 20.12.2034 | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD                  |                  | 01.01.2019 |            | OPPFYLT  | Nei                  |

      | 4       | GIFT_PARTNERSKAP,BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 15.03.2021 |            | OPPFYLT  | Nei                  |
      | 4       | UNDER_18_ÅR                                                  |                  | 15.03.2021 | 14.03.2039 | OPPFYLT  | Nei                  |

  Scenario: Skal ta med barn født etter opphørstidspunktet når vi opphører på alle barnas vilkår fra første utbetalingsmåned
    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD                 |                  | 01.01.2019 |            | OPPFYLT      | Nei                  |

      | 2       | GIFT_PARTNERSKAP                              |                  | 06.03.2014 |            | OPPFYLT      | Nei                  |
      | 2       | UNDER_18_ÅR                                   |                  | 06.03.2014 | 05.03.2032 | OPPFYLT      | Nei                  |
      | 2       | BOR_MED_SØKER,LOVLIG_OPPHOLD                  |                  | 01.01.2019 |            | OPPFYLT      | Nei                  |
      | 2       | BOSATT_I_RIKET                                |                  | 01.01.2019 |            | IKKE_OPPFYLT | Nei                  |

      | 3       | GIFT_PARTNERSKAP                              |                  | 21.12.2016 |            | OPPFYLT      | Nei                  |
      | 3       | UNDER_18_ÅR                                   |                  | 21.12.2016 | 20.12.2034 | OPPFYLT      | Nei                  |
      | 3       | BOR_MED_SØKER,LOVLIG_OPPHOLD                  |                  | 01.01.2019 |            | OPPFYLT      | Nei                  |
      | 3       | BOSATT_I_RIKET                                |                  | 01.01.2019 |            | IKKE_OPPFYLT | Nei                  |

      | 4       | GIFT_PARTNERSKAP,BOR_MED_SØKER,LOVLIG_OPPHOLD |                  | 15.03.2021 |            | OPPFYLT      | Nei                  |
      | 4       | UNDER_18_ÅR                                   |                  | 15.03.2021 | 14.03.2039 | OPPFYLT      | Nei                  |
      | 4       | BOSATT_I_RIKET                                |                  | 15.03.2021 |            | IKKE_OPPFYLT | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.02.2019 | 28.02.2032 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.02.2019 | 30.11.2034 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 4       | 1            | 01.04.2021 | 28.02.2039 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk | Gyldige begrunnelser       | Ugyldige begrunnelser |
      | 01.02.2019 |          | OPPHØR             |           | OPPHØR_IKKE_BOSATT_I_NORGE | OPPHØR_UTVANDRET      |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser       | Eøsbegrunnelser | Fritekster |
      | 01.02.2019 |          | OPPHØR_IKKE_BOSATT_I_NORGE |                 |            |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.02.2019 til -
      | Begrunnelse                | Type     | Gjelder søker | Barnas fødselsdatoer           | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp |
      | OPPHØR_IKKE_BOSATT_I_NORGE | STANDARD | Nei           | 06.03.14, 21.12.16 og 15.03.21 | 3           | januar 2019                          | NB      | 0     |

  Scenario: Skal ta med barn født etter opphørstidspunktet når vi opphører på alle barnas vilkår
    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD                 |                  | 01.01.2019 |            | OPPFYLT      | Nei                  |

      | 2       | GIFT_PARTNERSKAP                              |                  | 06.03.2014 |            | OPPFYLT      | Nei                  |
      | 2       | UNDER_18_ÅR                                   |                  | 06.03.2014 | 05.03.2032 | OPPFYLT      | Nei                  |
      | 2       | BOR_MED_SØKER,LOVLIG_OPPHOLD                  |                  | 01.01.2019 |            | OPPFYLT      | Nei                  |
      | 2       | BOSATT_I_RIKET                                |                  | 01.01.2019 | 15.07.2020 | OPPFYLT      | Nei                  |
      | 2       | BOSATT_I_RIKET                                |                  | 16.07.2020 |            | IKKE_OPPFYLT | Nei                  |

      | 3       | GIFT_PARTNERSKAP                              |                  | 21.12.2016 |            | OPPFYLT      | Nei                  |
      | 3       | UNDER_18_ÅR                                   |                  | 21.12.2016 | 20.12.2034 | OPPFYLT      | Nei                  |
      | 3       | BOR_MED_SØKER,LOVLIG_OPPHOLD                  |                  | 01.01.2019 |            | OPPFYLT      | Nei                  |
      | 3       | BOSATT_I_RIKET                                |                  | 01.01.2019 | 15.07.2020 | OPPFYLT      | Nei                  |
      | 3       | BOSATT_I_RIKET                                |                  | 16.07.2020 |            | IKKE_OPPFYLT | Nei                  |

      | 4       | GIFT_PARTNERSKAP,BOR_MED_SØKER,LOVLIG_OPPHOLD |                  | 15.03.2021 |            | OPPFYLT      | Nei                  |
      | 4       | UNDER_18_ÅR                                   |                  | 15.03.2021 | 14.03.2039 | OPPFYLT      | Nei                  |
      | 4       | BOSATT_I_RIKET                                |                  | 15.03.2021 |            | IKKE_OPPFYLT | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.02.2019 | 28.02.2032 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.02.2019 | 30.11.2034 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 4       | 1            | 01.04.2021 | 28.02.2039 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |

      | 2       | 2            | 01.02.2019 | 31.07.2020 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 2            | 01.02.2019 | 31.07.2020 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk | Gyldige begrunnelser | Ugyldige begrunnelser |
      | 01.08.2020 |          | OPPHØR             |           | OPPHØR_UTVANDRET     |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser | Eøsbegrunnelser | Fritekster |
      | 01.08.2020 |          | OPPHØR_UTVANDRET     |                 |            |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.08.2020 til -
      | Begrunnelse      | Type     | Gjelder søker | Barnas fødselsdatoer           | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp |
      | OPPHØR_UTVANDRET | STANDARD | Nei           | 06.03.14, 21.12.16 og 15.03.21 | 3           | juli 2020                            | NB      | 0     |

  Scenario: Skal ta med barn som flyttet inn hos søker etter opphørstidspunktet når vi opphører på søkers vilkår
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 3       | GIFT_PARTNERSKAP              |                  | 21.12.2016 |            | OPPFYLT  | Nei                  |
      | 3       | UNDER_18_ÅR                   |                  | 21.12.2016 | 20.12.2034 | OPPFYLT  | Nei                  |
      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.01.2019 |            | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER                 |                  | 01.07.2021 |            | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD                                |                  | 01.01.2019 | 15.07.2020 | OPPFYLT  | Nei                  |

      | 2       | GIFT_PARTNERSKAP                                             |                  | 06.03.2014 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                                  |                  | 06.03.2014 | 05.03.2032 | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD                  |                  | 01.01.2019 |            | OPPFYLT  | Nei                  |

      | 3       | GIFT_PARTNERSKAP                                             |                  | 21.12.2016 |            | OPPFYLT  | Nei                  |
      | 3       | UNDER_18_ÅR                                                  |                  | 21.12.2016 | 20.12.2034 | OPPFYLT  | Nei                  |
      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD                                |                  | 01.01.2019 |            | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER                                                |                  | 01.07.2021 |            | OPPFYLT  | Nei                  |

      | 4       | GIFT_PARTNERSKAP,BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 15.03.2021 |            | OPPFYLT  | Nei                  |
      | 4       | UNDER_18_ÅR                                                  |                  | 15.03.2021 | 14.03.2039 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.02.2019 | 28.02.2032 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.08.2021 | 30.11.2034 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 4       | 1            | 01.04.2021 | 28.02.2039 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |

      | 2       | 2            | 01.02.2019 | 31.07.2020 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk | Gyldige begrunnelser | Ugyldige begrunnelser |
      | 01.08.2020 |          | OPPHØR             |           | OPPHØR_UTVANDRET     |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser | Eøsbegrunnelser | Fritekster |
      | 01.08.2020 |          | OPPHØR_UTVANDRET     |                 |            |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.08.2020 til -
      | Begrunnelse      | Type     | Gjelder søker | Barnas fødselsdatoer           | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp |
      | OPPHØR_UTVANDRET | STANDARD | Ja            | 06.03.14, 21.12.16 og 15.03.21 | 0           | juli 2020                            | NB      | 0     |
