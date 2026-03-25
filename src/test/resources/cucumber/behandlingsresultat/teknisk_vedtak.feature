# language: no
# encoding: UTF-8

Egenskap: Teknisk vedtak

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | INNVILGET           | TEKNISK_ENDRING  | Nei                       | NASJONAL            | AVSLUTTET         |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 09.02.1999  |              |
      | 1            | 2       | BARN       | 13.02.2026  |              |

  Scenario: Ved utledning av behandlingsresultat i tekniske vedtak der barn blir lagt til så skal søknadsresultat bli satt til innvilget
    Og dagens dato er 25.03.2026
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 09.02.1999 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD                          |                  | 01.01.2026 |            | OPPFYLT  | Nei                  |                      |                  |

      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER,LOVLIG_OPPHOLD |                  | 13.02.2026 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 2       | GIFT_PARTNERSKAP                            |                  | 13.02.2026 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                                 |                  | 13.02.2026 | 12.02.2044 | OPPFYLT  | Nei                  |                      |                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 1       | 1            | 01.03.2026 | 31.01.2044 | 2572  | UTVIDET_BARNETRYGD | 100     | 2572 |
      | 2       | 1            | 01.03.2026 | 31.01.2044 | 2012  | ORDINÆR_BARNETRYGD | 100     | 2012 |

    Og når behandlingsresultatet er utledet for behandling 1

    Så forvent at behandlingsresultatet er INNVILGET på behandling 1