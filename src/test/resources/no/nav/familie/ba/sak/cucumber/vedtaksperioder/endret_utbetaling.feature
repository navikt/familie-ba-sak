# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder med endret utbetaling der endringstidspunkt påvirker periodene

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

  Scenario: Skal ikke lage utbetalingsperiode når andelene er endret til 0% og det ikke er delt bosted
    Gitt følgende behandlinger
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 24.12.1987  |
      | 1            | 3456    | BARN       | 02.12.2016  |

    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |                      |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 02.12.2016 |            | Oppfylt  |                      |
      | 3456    | UNDER_18_ÅR                                                     | 02.12.2016 | 01.12.2034 | Oppfylt  |                      |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId | Prosent |
      | 3456    | 01.01.2017 | 30.11.2034 | 1234  | 1            | 0       |

    Og med endrede utbetalinger
      | AktørId | Fra dato   | Til dato   | BehandlingId | Årsak             | Prosent | Avtaletidspunkt delt bosted |
      | 3456    | 01.01.2017 | 30.11.2034 | 1            | ETTERBETALING_3ÅR | 0       | 02.02.2017                  |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent følgende vedtaksperioder for behandling 1
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar            |
      | 01.01.2017 | 30.11.2034 | Opphør             | Endret utbetaling 0% |
      | 01.12.2034 |            | Opphør             | Opphør 18 år         |

  Scenario:  Skal lage utbetalingsperiode når andelene er endret til 0% og det er delt bosted
    Gitt følgende behandlinger
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 24.12.1987  |
      | 1            | 3456    | BARN       | 02.12.2016  |

    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |                      |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 02.12.2016 |            | Oppfylt  |                      |
      | 3456    | UNDER_18_ÅR                                                     | 02.12.2016 | 01.12.2034 | Oppfylt  |                      |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId | Prosent |
      | 3456    | 01.01.2017 | 30.11.2034 | 1234  | 1            | 0       |

    Og med endrede utbetalinger
      | AktørId | Fra dato   | Til dato   | BehandlingId | Årsak       | prosent | Avtaletidspunkt delt bosted |
      | 3456    | 01.01.2017 | 30.11.2034 | 1            | DELT_BOSTED | 0       | 02.02.2017                  |


    Når vedtaksperiodene genereres for behandling 1

    Så forvent følgende vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar       |
      | 01.01.2017 | 30.11.2034 | Utbetaling         | Delt bosted     |
      | 01.12.2034 |            | Opphør             | Barn er over 18 |

  Scenario: Skal ikke slå sammen vedtaksperiodene som ikke er innvilget dersom det er på grunn av endret utbetaling
    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | AVSLÅTT             | SØKNAD           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | BARN       | 02.02.2015  |
      | 1            | 2       | SØKER      | 17.04.1985  |

    Og dagens dato er 27.09.2023
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD                  |                  | 17.04.1985 |            | OPPFYLT  | Nei                  |

      | 1       | BOSATT_I_RIKET,GIFT_PARTNERSKAP,LOVLIG_OPPHOLD |                  | 02.02.2015 |            | OPPFYLT  | Nei                  |
      | 1       | UNDER_18_ÅR                                    |                  | 02.02.2015 | 01.02.2033 | OPPFYLT  | Nei                  |
      | 1       | BOR_MED_SØKER                                  |                  | 02.02.2015 | 15.12.2018 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.03.2015 | 31.12.2018 | 0     | ORDINÆR_BARNETRYGD | 0       | 970  |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent |
      | 1       | 1            | 01.03.2015 | 31.12.2018 | ETTERBETALING_3ÅR | 0       |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent følgende vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype |
      | 01.03.2015 | 31.12.2018 | OPPHØR             |
      | 01.01.2019 |            | OPPHØR             |

  Scenario: Skal ikke slå sammen vedtaksperiodene som ikke er innvilget dersom det er på grunn av endret utbetaling 3mnd
    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | AVSLÅTT             | SØKNAD           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | BARN       | 02.02.2015  |
      | 1            | 2       | SØKER      | 17.04.1985  |

    Og dagens dato er 27.09.2023
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                         | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD                  |                  | 17.04.1985 |            | OPPFYLT  | Nei                  |

      | 1       | BOSATT_I_RIKET,GIFT_PARTNERSKAP,LOVLIG_OPPHOLD |                  | 02.02.2015 |            | OPPFYLT  | Nei                  |
      | 1       | UNDER_18_ÅR                                    |                  | 02.02.2015 | 01.02.2033 | OPPFYLT  | Nei                  |
      | 1       | BOR_MED_SØKER                                  |                  | 02.02.2015 | 15.12.2018 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.03.2015 | 31.12.2018 | 0     | ORDINÆR_BARNETRYGD | 0       | 970  |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent |
      | 1       | 1            | 01.03.2015 | 31.12.2018 | ETTERBETALING_3MND | 0       |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent følgende vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype |
      | 01.03.2015 | 31.12.2018 | OPPHØR             |
      | 01.01.2019 |            | OPPHØR             |

  Scenario: Ved ingen endringer på endring utbetaling i en periode så skal alle barn som har begrunnelsen som en gyldig valg flettes inn.
    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 23.11.1983  |              |
      | 1            | 2       | BARN       | 17.08.2007  |              |
      | 1            | 3       | BARN       | 26.07.2009  |              |
    Og dagens dato er 15.05.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 3       |
      | 1            | 2       |
      | 1            | 1       |
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET                |                  | 12.11.1983 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | LOVLIG_OPPHOLD                |                  | 23.11.1983 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD            |                  | 01.02.2024 |            | OPPFYLT  | Nei                  |                      |                  |

      | 2       | UNDER_18_ÅR                   |                  | 17.08.2007 | 16.08.2025 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 17.08.2007 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | GIFT_PARTNERSKAP              |                  | 17.08.2007 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED      | 01.08.2023 | 20.03.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                 |                  | 21.03.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 26.07.2009 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | GIFT_PARTNERSKAP              |                  | 26.07.2009 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR                   |                  | 26.07.2009 | 25.07.2027 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER                 | DELT_BOSTED      | 01.08.2023 | 20.03.2025 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER                 |                  | 21.03.2025 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt | Avtaletidspunkt delt bosted |
      | 1       | 1            | 01.03.2024 | 30.11.2024 | ETTERBETALING_3MND | 0       | 26.03.2025       |                             |
      | 3       | 1            | 01.09.2023 | 31.03.2025 | DELT_BOSTED        | 0       | 26.03.2025       | 2023-08-01                  |
      | 2       | 1            | 01.09.2023 | 31.03.2025 | DELT_BOSTED        | 0       | 26.03.2025       | 2023-08-01                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 1       | 1            | 01.03.2024 | 30.11.2024 | 0     | UTVIDET_BARNETRYGD | 0       | 2516 |
      | 1       | 1            | 01.12.2024 | 31.03.2025 | 1258  | UTVIDET_BARNETRYGD | 50      | 2516 |
      | 1       | 1            | 01.04.2025 | 30.06.2027 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |
      | 2       | 1            | 01.09.2023 | 31.03.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1310 |
      | 2       | 1            | 01.04.2025 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.05.2025 | 31.07.2025 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 1            | 01.09.2023 | 31.03.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1310 |
      | 3       | 1            | 01.04.2025 | 30.04.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.05.2025 | 30.06.2027 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                                                                                                                                                  | Ugyldige begrunnelser |
      | 01.09.2023 | 29.02.2024 | UTBETALING         |                                | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING, ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY                                                                    |                       |
      | 01.03.2024 | 30.11.2024 | UTBETALING         |                                | ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TILBAKE_I_TID_KUN_UTVIDET_DEL_UTBETALING, ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY, INNVILGET_FAKTISK_SEPARASJON         |                       |
      | 01.12.2024 | 31.03.2025 | UTBETALING         |                                | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING, ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_KUN_UTVIDET_DEL, ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY |                       |
      | 01.04.2025 | 30.04.2025 | UTBETALING         |                                | INNVILGET_ENIGHET_OM_OPPHØR_AV_AVTALE_OM_DELT_BOSTED                                                                                                                                  |                       |
      | 01.05.2025 | 31.07.2025 | UTBETALING         |                                |                                                                                                                                                                                       |                       |
      | 01.08.2025 | 30.06.2027 | UTBETALING         |                                |                                                                                                                                                                                       |                       |
      | 01.07.2027 |            | OPPHØR             |                                |                                                                                                                                                                                       |                       |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                                                                                                                                                                  | Eøsbegrunnelser | Fritekster |
      | 01.09.2023 | 29.02.2024 | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING, ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY                                                                    |                 |            |
      | 01.03.2024 | 30.11.2024 | ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TILBAKE_I_TID_KUN_UTVIDET_DEL_UTBETALING, ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY, INNVILGET_FAKTISK_SEPARASJON         |                 |            |
      | 01.12.2024 | 31.03.2025 | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING, ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_KUN_UTVIDET_DEL, ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY |                 |            |
      | 01.04.2025 | 30.04.2025 | INNVILGET_ENIGHET_OM_OPPHØR_AV_AVTALE_OM_DELT_BOSTED                                                                                                                                  |                 |            |
      | 01.05.2025 | 31.07.2025 |                                                                                                                                                                                       |                 |            |
      | 01.08.2025 | 30.06.2027 |                                                                                                                                                                                       |                 |            |
      | 01.07.2027 |            |                                                                                                                                                                                       |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.09.2023 til 29.02.2024
      | Begrunnelse                                               | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING   | STANDARD |               | 17.08.07 og 26.07.09 | 2           | august 2023                          |         | 0     | 26.03.25         |                         | 01.08.23                    |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY | STANDARD |               | 17.08.07 og 26.07.09 | 2           | august 2023                          |         | 0     | 26.03.25         |                         |                             |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.03.2024 til 30.11.2024
      | Begrunnelse                                                                          | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet     | Avtaletidspunkt delt bosted |
      | ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_TILBAKE_I_TID_KUN_UTVIDET_DEL_UTBETALING | STANDARD | Ja            |                      | 0           | februar 2024                         |         | 0     | 26.03.25         | SØKER_HAR_RETT_MEN_FÅR_IKKE |                             |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY                            | STANDARD | Nei           | 17.08.07 og 26.07.09 | 2           | februar 2024                         |         | 0     | 26.03.25         | SØKER_HAR_RETT_MEN_FÅR_IKKE |                             |
      | INNVILGET_FAKTISK_SEPARASJON                                                         | STANDARD | Nei           | 17.08.07 og 26.07.09 | 2           | februar 2024                         |         | 0     |                  | SØKER_HAR_RETT_MEN_FÅR_IKKE |                             |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.12.2024 til 31.03.2025
      | Begrunnelse                                                       | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_ENDRET_UTBETALING           | STANDARD | Nei           | 17.08.07 og 26.07.09 | 2           | november 2024                        |         | 0     | 26.03.25         | SØKER_FÅR_UTVIDET       | 01.08.23                    |
      | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_MÅNEDER_KUN_UTVIDET_DEL | STANDARD | Ja            | 17.08.07 og 26.07.09 | 2           | november 2024                        |         | 1 258 | 26.03.25         | SØKER_FÅR_UTVIDET       |                             |
      | ENDRET_UTBETALINGSPERIODE_DELT_BOSTED_INGEN_UTBETALING_NY         | STANDARD | Nei           | 17.08.07 og 26.07.09 | 2           | november 2024                        |         | 0     | 26.03.25         | SØKER_FÅR_UTVIDET       |                             |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.04.2025 til 30.04.2025
      | Begrunnelse                                          | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | INNVILGET_ENIGHET_OM_OPPHØR_AV_AVTALE_OM_DELT_BOSTED | STANDARD |               | 17.08.07 og 26.07.09 | 2           | mars 2025                            |         | 3 532 |                  | SØKER_FÅR_UTVIDET       |                             |
