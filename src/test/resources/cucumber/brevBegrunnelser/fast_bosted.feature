# language: no
# encoding: UTF-8

Egenskap: Fast vs. delt bosted

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Fagsakstatus |
      | 1        | NORMAL     | OPPRETTET    |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 05.08.1991  |              |
      | 1            | 2       | BARN       | 18.01.2011  |              |
      | 1            | 3       | BARN       | 11.06.2012  |              |
      | 1            | 4       | BARN       | 11.06.2012  |              |

  Scenario: Fletter inn riktig barn når det er to barn med delt bosted og ett barn med fast bosted
    Og dagens dato er 21.03.2024
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 05.08.1991 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD            |                  | 05.01.2024 |            | OPPFYLT  | Nei                  |                      |                  |

      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 18.01.2011 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | UNDER_18_ÅR                   |                  | 18.01.2011 | 17.01.2029 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP              |                  | 18.01.2011 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER                 |                  | 05.01.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 4       | UNDER_18_ÅR                   |                  | 11.06.2012 | 10.06.2030 | OPPFYLT  | Nei                  |                      |                  |
      | 4       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 11.06.2012 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | GIFT_PARTNERSKAP              |                  | 11.06.2012 |            | OPPFYLT  | Nei                  |                      |                  |
      | 4       | BOR_MED_SØKER                 | DELT_BOSTED      | 05.01.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | BOSATT_I_RIKET                |                  | 11.06.2012 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | UNDER_18_ÅR                   |                  | 11.06.2012 | 10.06.2030 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP              |                  | 11.06.2012 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | LOVLIG_OPPHOLD                |                  | 06.12.2012 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                 | DELT_BOSTED      | 05.01.2024 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 1       | 1            | 01.02.2024 | 31.12.2028 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 1       | 1            | 01.01.2029 | 31.05.2030 | 1258  | UTVIDET_BARNETRYGD | 50      | 2516 |
      | 2       | 1            | 01.02.2024 | 31.12.2028 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 1            | 01.02.2024 | 29.02.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1510 |
      | 4       | 1            | 01.02.2024 | 29.02.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1510 |
      | 3       | 1            | 01.03.2024 | 31.05.2030 | 755   | ORDINÆR_BARNETRYGD | 50      | 1510 |
      | 4       | 1            | 01.03.2024 | 31.05.2030 | 755   | ORDINÆR_BARNETRYGD | 50      | 1510 |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak       | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 3, 4    | 1            | 01.02.2024 | 29.02.2024 | DELT_BOSTED | 0       | 28.02.2024       | 2024-01-05                  |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                           | Ugyldige begrunnelser |
      | 01.02.2024 | 29.02.2024 | UTBETALING         |                                | INNVILGET_RETTSAVGJØRELSE_DELT_BOSTED, INNVILGET_BOR_HOS_SØKER |                       |
      | 01.03.2024 | 31.12.2028 | UTBETALING         |                                |                                                                |                       |
      | 01.01.2029 | 31.05.2030 | UTBETALING         |                                |                                                                |                       |
      | 01.06.2030 |            | OPPHØR             |                                |                                                                |                       |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                                           | Eøsbegrunnelser | Fritekster |
      | 01.02.2024 | 29.02.2024 | INNVILGET_RETTSAVGJØRELSE_DELT_BOSTED, INNVILGET_BOR_HOS_SØKER |                 |            |
      | 01.03.2024 | 31.12.2028 |                                                                |                 |            |
      | 01.01.2029 | 31.05.2030 |                                                                |                 |            |
      | 01.06.2030 |            |                                                                |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.02.2024 til 29.02.2024
      | Begrunnelse                           | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søkers rett til utvidet |
      | INNVILGET_RETTSAVGJØRELSE_DELT_BOSTED | STANDARD | Nei           | 11.06.12 og 11.06.12 | 2           | januar 2024                          | NB      | 0     | SØKER_FÅR_UTVIDET        |
      | INNVILGET_BOR_HOS_SØKER               | STANDARD | Nei           | 18.01.11             | 1           | januar 2024                          | NB      | 1 510 | SØKER_FÅR_UTVIDET       |

