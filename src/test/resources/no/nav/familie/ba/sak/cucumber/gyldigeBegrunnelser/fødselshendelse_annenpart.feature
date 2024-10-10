# language: no
# encoding: UTF-8

Egenskap: Fødselshendelse hvor det hentes inn persongrunnlag for ANNENPART

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | INNVILGET           | FØDSELSHENDELSE  | Ja                        | NASJONAL            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | ANNENPART  | 04.09.1984  |
      | 1            | 2       | SØKER      | 14.06.1989  |
      | 1            | 3       | BARN       | 18.10.2023  |

  Scenario: Skal få vedtak med begrunnelse Innvliget fødselhendelse nyfødt barn første
    Og dagens dato er 27.10.2023
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD                                |                  | 18.10.2023 |            | OPPFYLT  | Nei                  |                      |

      | 3       | UNDER_18_ÅR                                                  |                  | 18.10.2023 | 17.10.2041 | OPPFYLT  | Nei                  |                      |
      | 3       | BOR_MED_SØKER,GIFT_PARTNERSKAP,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 18.10.2023 |            | OPPFYLT  | Nei                  |                      |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 3       | 1            | 01.11.2023 | 30.09.2029 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.10.2029 | 30.09.2041 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                         | Ugyldige begrunnelser |
      | 01.11.2023 | 30.09.2029 | UTBETALING         |                                | INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE |                       |
      | 01.10.2029 | 30.09.2041 | UTBETALING         |                                |                                              |                       |
      | 01.10.2041 |            | OPPHØR             |                                |                                              |                       |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                         | Eøsbegrunnelser | Fritekster |
      | 01.11.2023 | 30.09.2029 | INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE |                 |            |
      | 01.10.2029 | 30.09.2041 |                                              |                 |            |
      | 01.10.2041 |            |                                              |                 |            |

    Så forvent følgende brevperioder for behandling 1
      | Brevperiodetype | Fra dato      | Til dato           | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |
      | UTBETALING      | november 2023 | til september 2029 | 1766  | 1                          | 18.10.23            | du                     |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.11.2023 til 30.09.2029
      | Begrunnelse                                  | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE | STANDARD |               | 18.10.23             | 1           | oktober 2023                         |         | 1 766 |                  |                         |                             |