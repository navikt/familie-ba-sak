# language: no
# encoding: UTF-8

Egenskap: Innvilget enslig mindreårlig ved oppfylt lovlig opphold

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype             | Status    |
      | 1        | BARN_ENSLIG_MINDREÅRIG | OPPRETTET |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | BARN       | 09.10.2008  |              |

  Scenario: Skal gi utvidet etter endring i lovlig opphold
    Og dagens dato er 21.10.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 1       |
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår             | Utdypende vilkår         | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser                                    | Vurderes etter   |
      | 1       | BOSATT_I_RIKET     |                          | 09.10.2008 |            | OPPFYLT      | Nei                  |                                                         | NASJONALE_REGLER |
      | 1       | LOVLIG_OPPHOLD     |                          | 09.10.2008 | 10.02.2025 | IKKE_OPPFYLT | Nei                  |                                                         | NASJONALE_REGLER |
      | 1       | UNDER_18_ÅR        |                          | 09.10.2008 | 08.10.2026 | OPPFYLT      | Nei                  |                                                         |                  |
      | 1       | GIFT_PARTNERSKAP   |                          | 09.10.2008 |            | OPPFYLT      | Nei                  |                                                         |                  |
      | 1       | LOVLIG_OPPHOLD     | VURDERING_ANNET_GRUNNLAG | 11.02.2025 | 10.04.2025 | IKKE_OPPFYLT | Ja                   | AVSLAG_OPPHOLDSTILLATELSE_UTLENDINGSLOVEN_34_ANDRE_LEDD | NASJONALE_REGLER |
      | 1       | BOR_MED_SØKER      |                          | 11.02.2025 |            | OPPFYLT      | Nei                  |                                                         | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD |                          | 11.02.2025 |            | OPPFYLT      | Nei                  |                                                         |                  |
      | 1       | LOVLIG_OPPHOLD     |                          | 11.04.2025 |            | OPPFYLT      | Nei                  |                                                         | NASJONALE_REGLER |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 1       | 1            | 01.05.2025 | 30.09.2026 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 1       | 1            | 01.05.2025 | 30.09.2026 | 2516  | UTVIDET_BARNETRYGD | 100     | 2516 |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                  | Ugyldige begrunnelser |
      | 01.05.2025 | 30.09.2026 | UTBETALING         |                                | INNVILGET_ENSLIG_MINDREÅRIG_BOR_ALENE |                       |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                  | Eøsbegrunnelser | Fritekster |
      | 01.05.2025 | 30.09.2026 | INNVILGET_ENSLIG_MINDREÅRIG_BOR_ALENE |                 |            |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 1 i periode 01.05.2025 til 30.09.2026
      | Begrunnelse                           | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | INNVILGET_ENSLIG_MINDREÅRIG_BOR_ALENE | STANDARD |               | 09.10.08             | 1           | april 2025                           |         | 4 484 |                  | søker_Får_Utvidet       |                             |