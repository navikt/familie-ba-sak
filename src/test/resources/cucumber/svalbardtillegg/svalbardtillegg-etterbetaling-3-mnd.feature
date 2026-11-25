# language: no
# encoding: UTF-8

Egenskap: Svalbardtillegg i periode uten utbetaling på grunn av etterbetaling 3 måneder

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | Behandlingsresultat | Behandlingsårsak |
      | 1            | 1        | DELVIS_INNVILGET    | SØKNAD           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 08.02.2003  |
      | 1            | 2       | BARN       | 14.12.2025  |

  Scenario: Skal ikke splitte vedtaksperioden når søker og barn flytter til Svalbard i periode uten utbetaling
    Og dagens dato er 24.11.2026
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 2       |
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår   | Fra dato   | Til dato   | Resultat |
      | 1       | LOVLIG_OPPHOLD               |                    | 08.02.2003 |            | OPPFYLT  |
      | 1       | BOSATT_I_RIKET               |                    | 08.02.2003 | 02.06.2026 | OPPFYLT  |
      | 1       | BOSATT_I_RIKET               | BOSATT_PÅ_SVALBARD | 03.06.2026 |            | OPPFYLT  |

      | 2       | LOVLIG_OPPHOLD,BOR_MED_SØKER |                    | 14.12.2025 |            | OPPFYLT  |
      | 2       | GIFT_PARTNERSKAP             |                    | 14.12.2025 |            | OPPFYLT  |
      | 2       | UNDER_18_ÅR                  |                    | 14.12.2025 | 13.12.2043 | OPPFYLT  |
      | 2       | BOSATT_I_RIKET               |                    | 14.12.2025 | 02.06.2026 | OPPFYLT  |
      | 2       | BOSATT_I_RIKET               | BOSATT_PÅ_SVALBARD | 03.06.2026 |            | OPPFYLT  |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent | Søknadstidspunkt |
      | 2       | 1            | 01.01.2026 | 31.07.2026 | ETTERBETALING_3MND | 0       | 24.11.2026       |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.01.2026 | 31.07.2026 | 0     | ORDINÆR_BARNETRYGD | 0       | 2012 |
      | 2       | 1            | 01.08.2026 | 30.11.2043 | 2012  | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 2       | 1            | 01.08.2026 | 30.11.2043 | 512   | SVALBARDTILLEGG    | 100     | 512  |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent følgende vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.01.2026 | 31.07.2026 | OPPHØR             |           |
      | 01.08.2026 | 30.11.2043 | UTBETALING         |           |
      | 01.12.2043 |            | OPPHØR             |           |
