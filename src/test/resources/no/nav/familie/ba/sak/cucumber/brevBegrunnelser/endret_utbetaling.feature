# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelse ved endret utbeetaling

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | Nei                       | NASJONAL            |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 24.08.1987  |
      | 1            | 2       | BARN       | 15.06.2012  |
      | 1            | 3       | BARN       | 16.06.2016  |

  Scenario: Skal gi to brevbegrunnelse når vi har delt bosted med flere avtaletidspunkt samme måned.
    Og følgende dagens dato 24.10.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser |
      | 1       | UTVIDET_BARNETRYGD,BOSATT_I_RIKET              |                  | 24.08.1987 |            | OPPFYLT  | Nei                  |                      |
      | 1       | LOVLIG_OPPHOLD                                 |                  | 15.02.2022 | 15.05.2022 | OPPFYLT  | Nei                  |                      |

      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,GIFT_PARTNERSKAP |                  | 15.06.2012 |            | OPPFYLT  | Nei                  |                      |
      | 2       | BOR_MED_SØKER                                  |                  | 15.06.2012 | 03.04.2022 | OPPFYLT  | Nei                  |                      |
      | 2       | UNDER_18_ÅR                                    |                  | 15.06.2012 | 14.06.2030 | OPPFYLT  | Nei                  |                      |
      | 2       | BOR_MED_SØKER                                  | DELT_BOSTED      | 04.04.2022 |            | OPPFYLT  | Nei                  |                      |

      | 3       | BOSATT_I_RIKET,GIFT_PARTNERSKAP,LOVLIG_OPPHOLD |                  | 16.06.2016 |            | OPPFYLT  | Nei                  |                      |
      | 3       | BOR_MED_SØKER                                  |                  | 16.06.2016 | 01.02.2022 | OPPFYLT  | Nei                  |                      |
      | 3       | UNDER_18_ÅR                                    |                  | 16.06.2016 | 15.06.2034 | OPPFYLT  | Nei                  |                      |
      | 3       | BOR_MED_SØKER                                  | DELT_BOSTED      | 02.02.2022 |            | OPPFYLT  | Nei                  |                      |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.03.2022 | 30.04.2022 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 1       | 1            | 01.05.2022 | 31.05.2022 | 527   | UTVIDET_BARNETRYGD | 50      | 1054 |
      | 2       | 1            | 01.03.2022 | 31.05.2022 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.03.2022 | 31.05.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |

    Og med endrede utbetalinger for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak       | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 3       | 1            | 01.03.2022 | 31.05.2022 | DELT_BOSTED | 100     | 02.02.2022       | 02.02.2022                  |
      | 2       | 1            | 01.05.2022 | 31.05.2022 | DELT_BOSTED | 100     | 04.04.2022       | 04.04.2022                  |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                    | Ugyldige begrunnelser |
      | 01.03.2022 | 30.04.2022 | UTBETALING         |                                | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING |                       |
      | 01.05.2022 | 31.05.2022 | UTBETALING         |                                | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING |                       |
      | 01.06.2022 |            | OPPHØR             |                                |                                                         |                       |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                                    | Eøsbegrunnelser | Fritekster |
      | 01.03.2022 | 30.04.2022 | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING |                 |            |
      | 01.05.2022 | 31.05.2022 | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING |                 |            |
      | 01.06.2022 |            |                                                         |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.03.2022 til 30.04.2022
      | Begrunnelse                                             | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING | STANDARD | Nei           | 16.06.16             | 1           | februar 2022                         | NB      | 1 676 | 02.02.22         | SØKER_FÅR_UTVIDET       | 02.02.22                    |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.05.2022 til 31.05.2022
      | Begrunnelse                                             | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING | STANDARD | Nei           | 16.06.16             | 1           | april 2022                           | NB      | 1 676 | 02.02.22         | SØKER_FÅR_UTVIDET       | 02.02.22                    |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING | STANDARD | Nei           | 15.06.12             | 1           | april 2022                           | NB      | 1 054 | 02.02.22         | SØKER_FÅR_UTVIDET       | 04.04.22                    |