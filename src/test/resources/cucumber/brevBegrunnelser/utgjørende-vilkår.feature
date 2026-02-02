# language: no
# encoding: UTF-8

Egenskap: Utgjørende vilkår

  Scenario: Vilkår som blir oppfylt måneden før vedtaksperioden skal regnes som utgjørende i nåværende vedtaksperiode

    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status    |
      | 1        | NORMAL     | OPPRETTET |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 16.12.1993  |              |
      | 1            | 2       | BARN       | 01.10.2017  |              |
      | 1            | 3       | BARN       | 17.04.2020  |              |
      | 1            | 4       | BARN       | 28.09.2023  |              |
    Og dagens dato er 01.02.2026
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 4       |
      | 1            | 3       |
      | 1            | 2       |
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD               |                  | 01.10.2024 | 30.09.2025 | IKKE_OPPFYLT | Nei                  |                      | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET               |                  | 01.10.2024 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |
      | 1       | LOVLIG_OPPHOLD               |                  | 01.10.2025 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |

      | 2       | GIFT_PARTNERSKAP             |                  | 01.10.2017 |            | OPPFYLT      | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                  |                  | 01.10.2017 | 30.09.2035 | OPPFYLT      | Nei                  |                      |                  |
      | 2       | LOVLIG_OPPHOLD               |                  | 01.10.2024 | 30.09.2025 | IKKE_OPPFYLT | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 01.10.2024 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |
      | 2       | LOVLIG_OPPHOLD               |                  | 01.10.2025 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR                  |                  | 17.04.2020 | 16.04.2038 | OPPFYLT      | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP             |                  | 17.04.2020 |            | OPPFYLT      | Nei                  |                      |                  |
      | 3       | LOVLIG_OPPHOLD               |                  | 01.10.2024 | 30.09.2025 | IKKE_OPPFYLT | Nei                  |                      | NASJONALE_REGLER |
      | 3       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 01.10.2024 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |
      | 3       | LOVLIG_OPPHOLD               |                  | 01.10.2025 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |

      | 4       | GIFT_PARTNERSKAP             |                  | 28.09.2023 |            | OPPFYLT      | Nei                  |                      |                  |
      | 4       | UNDER_18_ÅR                  |                  | 28.09.2023 | 27.09.2041 | OPPFYLT      | Nei                  |                      |                  |
      | 4       | LOVLIG_OPPHOLD               |                  | 01.10.2024 | 30.09.2025 | IKKE_OPPFYLT | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BOSATT_I_RIKET,BOR_MED_SØKER |                  | 01.10.2024 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |
      | 4       | LOVLIG_OPPHOLD               |                  | 01.10.2025 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 2       | 1            | 01.11.2025 | 31.01.2026 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 2       | 1            | 01.02.2026 | 30.09.2035 | 2012  | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 3       | 1            | 01.11.2025 | 31.01.2026 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 1            | 01.02.2026 | 31.03.2038 | 2012  | ORDINÆR_BARNETRYGD | 100     | 2012 |
      | 4       | 1            | 01.11.2025 | 31.01.2026 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 4       | 1            | 01.02.2026 | 31.08.2041 | 2012  | ORDINÆR_BARNETRYGD | 100     | 2012 |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                | Ugyldige begrunnelser |
      | 01.11.2025 | 31.01.2026 | UTBETALING         |                                | INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER |                       |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                | Eøsbegrunnelser | Fritekster |
      | 01.11.2025 | 31.01.2026 | INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER |                 |            |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 1 i periode 01.11.2025 til 31.01.2026
      | Begrunnelse                         | Type     | Gjelder søker | Barnas fødselsdatoer           | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp |
      | INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER | STANDARD | Ja            | 01.10.17, 17.04.20 og 28.09.23 | 3           | oktober 2025                         | NB      | 5 904 |