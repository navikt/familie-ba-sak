# language: no
# encoding: UTF-8

Egenskap: Eksplisitt avslag på barn som hadde utbetaling i forrige behandling

  Scenario: Avslagsbegrunnelse fra vilkårsvurderingen skal ikke forhåndsutfylles på utbetalingsperiode med reduksjon fra forrige behandling
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           |
      | 2            | 1        | 1                   | AVSLÅTT_OG_ENDRET   | SØKNAD           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 15.10.1988  |
      | 1            | 2       | BARN       | 01.02.2016  |
      | 1            | 3       | BARN       | 01.03.2018  |
      | 2            | 1       | SØKER      | 15.10.1988  |
      | 2            | 2       | BARN       | 01.02.2016  |
      | 2            | 3       | BARN       | 01.03.2018  |

    Og dagens dato er 24.06.2024
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 2            | 3       |

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               | 15.05.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | UNDER_18_ÅR                                 | 01.02.2016 | 31.01.2034 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP                            | 01.02.2016 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER | 15.05.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR                                 | 01.03.2018 | 29.02.2036 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP                            | 01.03.2018 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER | 15.05.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og kopier vilkårresultater fra behandling 1 til behandling 2

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser  | Vurderes etter   |
      | 3       | UNDER_18_ÅR                  | 01.03.2018 | 29.02.2036 | OPPFYLT      | Nei                  |                       |                  |
      | 3       | GIFT_PARTNERSKAP             | 01.03.2018 |            | OPPFYLT      | Nei                  |                       |                  |
      | 3       | LOVLIG_OPPHOLD,BOR_MED_SØKER | 15.05.2023 |            | OPPFYLT      | Nei                  |                       | NASJONALE_REGLER |
      | 3       | BOSATT_I_RIKET               | 15.05.2023 | 29.02.2036 | IKKE_OPPFYLT | Ja                   | AVSLAG_BOSATT_I_RIKET | NASJONALE_REGLER |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.06.2023 | 31.01.2034 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.06.2023 | 29.02.2036 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 2            | 01.06.2023 | 31.01.2034 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent følgende vedtaksperioder for behandling 2
      | Fra dato   | Til dato   | Vedtaksperiodetype                                      | Begrunnelser          |
      | 01.06.2023 | 31.01.2034 | UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING |                       |
      | 01.02.2034 |            | OPPHØR                                                  |                       |
      | 01.06.2023 | 29.02.2036 | AVSLAG                                                  | AVSLAG_BOSATT_I_RIKET |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.06.2023 til 29.02.2036
      | Begrunnelse           | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp |
      | AVSLAG_BOSATT_I_RIKET | STANDARD | Nei           | 01.03.18             | 1           | mai 2023                             | NB      | 0     |

    Så forvent følgende brevperioder for behandling 2
      | Brevperiodetype  | Fra dato  | Til dato                   | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |
      | INGEN_UTBETALING | juni 2023 | "til og med februar 2036 " | 1766  | 1                          | 01.02.16            | du                     |
