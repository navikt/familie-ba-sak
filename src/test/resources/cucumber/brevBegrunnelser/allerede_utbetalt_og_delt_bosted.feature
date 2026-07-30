# language: no
# encoding: UTF-8

Egenskap: Brevperioder: Allerede utbetalt og delt bosted med utvidet barnetrygd

  Scenario: Barn skal ikke vises i brevperiode for allerede utbetalt uten utvidet barnetrygd, men skal vises fra første delt bosted-periode og videre

    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        | DELVIS_INNVILGET    | SØKNAD           | Nei                       | NASJONAL            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 01.01.1990  |
      | 1            | 2       | BARN       | 01.01.2020  |

    Og dagens dato er 01.06.2026
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 01.12.2025 |            | OPPFYLT  | Nei                  | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD            |                  | 01.04.2026 |            | OPPFYLT  | Nei                  |                  |

      | 2       | UNDER_18_ÅR                   |                  | 01.01.2020 | 31.12.2037 | OPPFYLT  | Nei                  |                  |
      | 2       | GIFT_PARTNERSKAP              |                  | 01.01.2020 |            | OPPFYLT  | Nei                  |                  |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 01.12.2025 |            | OPPFYLT  | Nei                  | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                 |                  | 01.12.2025 | 28.02.2026 | OPPFYLT  | Nei                  | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED      | 01.03.2026 |            | OPPFYLT  | Nei                  | NASJONALE_REGLER |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.01.2026 | 28.02.2026 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 1            | 01.03.2026 | 31.05.2026 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 1            | 01.06.2026 | 31.12.2037 | 2068  | ORDINÆR_BARNETRYGD | 100     | 2068 |
      | 1       | 1            | 01.04.2026 | 31.05.2026 | 0     | UTVIDET_BARNETRYGD | 0       | 2516 |
      | 1       | 1            | 01.06.2026 | 31.12.2037 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 2       | 1            | 01.01.2026 | 28.02.2026 | ALLEREDE_UTBETALT | 0       | 01.01.2026       |                             |
      | 2       | 1            | 01.03.2026 | 31.05.2026 | DELT_BOSTED       | 0       | 01.03.2026       | 01.01.2026                  |
      | 1       | 1            | 01.04.2026 | 31.05.2026 | DELT_BOSTED       | 0       | 01.03.2026       | 01.01.2026                  |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Gyldige begrunnelser                                      |
      | 01.03.2026 | 31.03.2026 | UTBETALING         | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY |
      | 01.04.2026 | 30.04.2026 | UTBETALING         | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY |
      | 01.05.2026 | 31.05.2026 | UTBETALING         | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY |
      | 01.06.2026 | 31.12.2037 | UTBETALING         | ETTER_ENDRET_UTBETALING_HAR_AVTALE_DELT_BOSTED            |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                                      |
      | 01.03.2026 | 31.03.2026 | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY |
      | 01.04.2026 | 30.04.2026 | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY |
      | 01.05.2026 | 31.05.2026 | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY |
      | 01.06.2026 | 31.12.2037 | ETTER_ENDRET_UTBETALING_HAR_AVTALE_DELT_BOSTED            |

    Så forvent følgende brevperioder for behandling 1
      | Brevperiodetype | Fra dato   | Til dato          | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |
      | UTBETALING      | mars 2026  | til mars 2026     | 0     | 1                          | 01.01.20            | du                     |
      | UTBETALING      | april 2026 | til april 2026    | 0     | 1                          | 01.01.20            | du                     |
      | UTBETALING      | mai 2026   | til mai 2026      | 0     | 1                          | 01.01.20            | du                     |
      | UTBETALING      | juni 2026  | til desember 2037 | 4584  | 1                          | 01.01.20            | du                     |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 1 i periode 01.03.2026 til 31.03.2026
      | Begrunnelse                                               | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Beløp | Søknadstidspunkt |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY | STANDARD | Nei           | 01.01.20             | 1           | februar 2026                         | 0     | 01.03.26         |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 1 i periode 01.04.2026 til 30.04.2026
      | Begrunnelse                                               | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Beløp | Søknadstidspunkt | Søkers rett til utvidet     |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY | STANDARD | Nei           | 01.01.20             | 1           | mars 2026                            | 0     | 01.03.26         | SØKER_HAR_RETT_MEN_FÅR_IKKE |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 1 i periode 01.05.2026 til 31.05.2026
      | Begrunnelse                                               | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Beløp | Søknadstidspunkt | Søkers rett til utvidet     |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY | STANDARD | Nei           | 01.01.20             | 1           | april 2026                           | 0     | 01.03.26         | SØKER_HAR_RETT_MEN_FÅR_IKKE |
