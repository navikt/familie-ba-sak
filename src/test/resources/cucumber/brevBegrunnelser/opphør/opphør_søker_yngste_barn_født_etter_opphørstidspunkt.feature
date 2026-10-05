# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser ved opphør på søkers vilkår når yngste barn er født etter opphørstidspunktet

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak     | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | INNVILGET_OG_ENDRET | ENDRE_MIGRERINGSDATO | Nei                       | NASJONAL            |
      | 2            | 1        | 1                   | OPPHØRT             | NYE_OPPLYSNINGER     | Nei                       | NASJONAL            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 14.03.1983  |
      | 1            | 2       | BARN       | 03.03.2015  |
      | 1            | 3       | BARN       | 10.02.2017  |
      | 1            | 4       | BARN       | 29.07.2021  |
      | 2            | 1       | SØKER      | 14.03.1983  |
      | 2            | 2       | BARN       | 03.03.2015  |
      | 2            | 3       | BARN       | 10.02.2017  |
      | 2            | 4       | BARN       | 29.07.2021  |

  Scenario: Skal ta med barn født etter opphørstidspunktet i opphørsbegrunnelse som gjelder søker
    Og dagens dato er 05.10.2026
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                  | 01.06.2020 |            | OPPFYLT  | Nei                  |

      | 2       | GIFT_PARTNERSKAP                            |                  | 03.03.2015 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                 |                  | 03.03.2015 | 02.03.2033 | OPPFYLT  | Nei                  |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER |                  | 01.06.2020 |            | OPPFYLT  | Nei                  |

      | 3       | UNDER_18_ÅR                                 |                  | 10.02.2017 | 09.02.2035 | OPPFYLT  | Nei                  |
      | 3       | GIFT_PARTNERSKAP                            |                  | 10.02.2017 |            | OPPFYLT  | Nei                  |
      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER |                  | 01.06.2020 |            | OPPFYLT  | Nei                  |

      | 4       | BOR_MED_SØKER,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 29.07.2021 |            | OPPFYLT  | Nei                  |
      | 4       | GIFT_PARTNERSKAP                            |                  | 29.07.2021 |            | OPPFYLT  | Nei                  |
      | 4       | UNDER_18_ÅR                                 |                  | 29.07.2021 | 28.07.2039 | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.06.2020 | 02.07.2020 | OPPFYLT  | Nei                  |

      | 2       | UNDER_18_ÅR                   |                  | 03.03.2015 | 02.03.2033 | OPPFYLT  | Nei                  |
      | 2       | GIFT_PARTNERSKAP              |                  | 03.03.2015 |            | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER                 |                  | 01.06.2020 | 02.07.2020 | OPPFYLT  | Nei                  |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 01.06.2020 |            | OPPFYLT  | Nei                  |

      | 3       | GIFT_PARTNERSKAP              |                  | 10.02.2017 |            | OPPFYLT  | Nei                  |
      | 3       | UNDER_18_ÅR                   |                  | 10.02.2017 | 09.02.2035 | OPPFYLT  | Nei                  |
      | 3       | BOR_MED_SØKER                 |                  | 01.06.2020 | 02.07.2020 | OPPFYLT  | Nei                  |
      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 01.06.2020 |            | OPPFYLT  | Nei                  |

      | 4       | UNDER_18_ÅR                   |                  | 29.07.2021 | 28.07.2039 | OPPFYLT  | Nei                  |
      | 4       | BOSATT_I_RIKET,BOR_MED_SØKER  |                  | 29.07.2021 |            | OPPFYLT  | Nei                  |
      | 4       | LOVLIG_OPPHOLD                |                  | 29.07.2021 |            | OPPFYLT  | Nei                  |
      | 4       | GIFT_PARTNERSKAP              |                  | 29.07.2021 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.07.2020 | 28.02.2033 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.07.2020 | 31.01.2035 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 4       | 1            | 01.08.2021 | 30.06.2039 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |

      | 2       | 2            | 01.07.2020 | 31.07.2020 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 2            | 01.07.2020 | 31.07.2020 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk | Gyldige begrunnelser                                                            | Ugyldige begrunnelser |
      | 01.08.2020 |          | OPPHØR             |           | OPPHØR_UTVANDRET, OPPHØR_IKKE_OPPHOLDSTILLATELSE, OPPHØR_BARN_FLYTTET_FRA_SØKER |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser                            | Eøsbegrunnelser | Fritekster |
      | 01.08.2020 |          | OPPHØR_UTVANDRET, OPPHØR_BARN_FLYTTET_FRA_SØKER |                 |            |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.08.2020 til -
      | Begrunnelse                   | Type     | Gjelder søker | Barnas fødselsdatoer           | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp |
      | OPPHØR_UTVANDRET              | STANDARD | Ja            | 03.03.15, 10.02.17 og 29.07.21 | 0           | juli 2020                            | NB      | 0     |
      | OPPHØR_BARN_FLYTTET_FRA_SØKER | STANDARD | Nei           | 03.03.15 og 10.02.17           | 2           | juli 2020                            | NB      | 0     |

    Så forvent følgende brevperioder for behandling 2
      | Brevperiodetype  | Fra dato    | Til dato | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |
      | INGEN_UTBETALING | august 2020 |          | 0     | 0                          |                     | du                     |
