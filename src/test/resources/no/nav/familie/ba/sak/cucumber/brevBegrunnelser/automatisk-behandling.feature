# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser ved automatiske behandlinger

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk |
      | 1            | 1        |                     | INNVILGET           | FØDSELSHENDELSE  | Ja                        |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 12.08.1995  |
      | 1            | 2       | BARN       | 04.06.2023  |

  Scenario: Skal sette riktige felter for automatisk fødselshendelse
    Og dagens dato er 05.06.2023
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2       | GIFT_PARTNERSKAP,BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER |                  | 04.06.2023 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                                  |                  | 04.06.2023 | 03.06.2041 | OPPFYLT  | Nei                  |

      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET                                |                  | 12.08.1995 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.07.2023 | 31.05.2029 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.06.2029 | 31.05.2041 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Når vedtaksperiodene genereres for behandling 1

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                         | Eøsbegrunnelser | Fritekster |
      | 01.07.2023 | 31.05.2029 | INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.07.2023 til 31.05.2029
      | Begrunnelse                                  | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE | Nei           | 04.06.23             | 1           | juni 2023                            | NB      | 1 766 |                  | SØKER_HAR_IKKE_RETT     |

  Scenario: Småbarnstillegg
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | Nei                       |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | SMÅBARNSTILLEGG  | Ja                        |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 12.01.1996  |
      | 1            | 2       | BARN       | 11.10.2021  |
      | 2            | 1       | SØKER      | 12.01.1996  |
      | 2            | 2       | BARN       | 11.10.2021  |

    Og dagens dato er 11.10.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,UTVIDET_BARNETRYGD,LOVLIG_OPPHOLD             |                  | 11.10.2021 |            | OPPFYLT  | Nei                  |

      | 2       | UNDER_18_ÅR                                                  |                  | 11.10.2021 | 10.10.2039 | OPPFYLT  | Nei                  |
      | 2       | LOVLIG_OPPHOLD,BOR_MED_SØKER,GIFT_PARTNERSKAP,BOSATT_I_RIKET |                  | 11.10.2021 |            | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,UTVIDET_BARNETRYGD             |                  | 11.10.2021 |            | OPPFYLT  | Nei                  |

      | 2       | UNDER_18_ÅR                                                  |                  | 11.10.2021 | 10.10.2039 | OPPFYLT  | Nei                  |
      | 2       | BOR_MED_SØKER,GIFT_PARTNERSKAP,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 11.10.2021 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.11.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     | 1654 |
      | 2       | 1            | 01.01.2022 | 30.09.2027 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 1            | 01.10.2027 | 30.09.2039 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 1       | 1            | 01.11.2021 | 30.09.2039 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 1            | 01.10.2022 | 31.10.2024 | 660   | SMÅBARNSTILLEGG    | 100     | 660  |

      | 2       | 2            | 01.11.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     | 1654 |
      | 2       | 2            | 01.01.2022 | 30.09.2027 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 2            | 01.10.2027 | 30.09.2039 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 1       | 2            | 01.11.2021 | 30.09.2039 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 2            | 01.10.2022 | 31.10.2023 | 660   | SMÅBARNSTILLEGG    | 100     | 660  |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Gyldige begrunnelser                                       | Ugyldige begrunnelser |
      | 01.11.2023 | 30.09.2027 | UTBETALING         |           | REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD |                       |
      | 01.10.2027 | 30.09.2039 | UTBETALING         |           |                                                            |                       |
      | 01.10.2039 |            | OPPHØR             |           |                                                            |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                                       | Eøsbegrunnelser | Fritekster |
      | 01.11.2023 | 30.09.2027 | REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.11.2023 til 30.09.2027
      | Begrunnelse                                                | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD | Ja            | 11.10.21             | 1           | oktober 2023                         | NB      | 2 730 |                  | SØKER_FÅR_UTVIDET       |