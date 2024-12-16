# language: no
# encoding: UTF-8

Egenskap: Etter endret utbetaling som ikke gjelder søker

  Scenario: Etter endret utbetaling begrunnelse ikke gjelder søker og ikke er allerede utbetalt skal bare ta med barn som får utbetalt

    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status    |
      | 1        | NORMAL     | OPPRETTET |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 03.12.1986  |              |
      | 1            | 2       | BARN       | 27.08.2006  |              |
      | 1            | 3       | BARN       | 08.11.2010  |              |

    Og dagens dato er 15.12.2024
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 3       |
      | 1            | 2       |
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                  | 22.11.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | UNDER_18_ÅR                                 |                  | 27.08.2006 | 26.08.2024 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP                            |                  | 27.08.2006 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 21.11.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR                                 |                  | 08.11.2010 | 07.11.2028 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP                            |                  | 08.11.2010 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 21.11.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                               |                  | 22.11.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 1            | 01.12.2023 | 31.07.2024 | ETTERBETALING_3MND | 0       | 10.11.2024       |                             |
      | 3       | 1            | 01.12.2023 | 31.07.2024 | ETTERBETALING_3MND | 0       | 10.11.2024       |                             |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 2       | 1            | 01.12.2023 | 31.07.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1310 |
      | 3       | 1            | 01.12.2023 | 31.07.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1310 |
      | 3       | 1            | 01.08.2024 | 31.08.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 1            | 01.09.2024 | 31.10.2028 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                                | Ugyldige begrunnelser |
      | 01.12.2023 | 31.07.2024 | OPPHØR             |                                | ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TILBAKE_I_TID           |                       |
      | 01.08.2024 | 31.08.2024 | UTBETALING         |                                | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TREDJELANDSBORGER |                       |
      | 01.09.2024 | 31.10.2028 | UTBETALING         |                                |                                                                     |                       |
      | 01.11.2028 |            | OPPHØR             |                                |                                                                     |                       |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                                                | Eøsbegrunnelser | Fritekster |
      | 01.12.2023 | 31.07.2024 | ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TILBAKE_I_TID           |                 |            |
      | 01.08.2024 | 31.08.2024 | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TREDJELANDSBORGER |                 |            |
      | 01.09.2024 | 31.10.2028 |                                                                     |                 |            |
      | 01.11.2028 |            |                                                                     |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.08.2024 til 31.08.2024
      | Begrunnelse                                                         | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TREDJELANDSBORGER | STANDARD | Nei           | 08.11.10             | 1           | juli 2024                            |         | 1 510 | 10.11.24         |                         |                             |