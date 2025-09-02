# language: no
# encoding: UTF-8

Egenskap: Opphør når alle vilkår har samme tom, som f.eks ved dødsfall

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype  | Status  |
      | 1        | INSTITUSJON | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | ENDRET_UTBETALING   | SATSENDRING      | Ja                        | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | OPPHØRT             | NYE_OPPLYSNINGER | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | BARN       | 26.12.2007  |              |
      | 2            | 1       | BARN       | 26.12.2007  | 13.07.2024   |

  Scenario: Dersom alle vilkår har samme tom og ingenting annet er endret i behandlingen skal vi få et opphør fra og med mnd etter tom
    Og dagens dato er 02.08.2024
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 1       |
      | 2            | 1       |
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | GIFT_PARTNERSKAP                            |                  | 26.12.2007 |            | OPPFYLT  | Nei                  |                      |                  |
      | 1       | UNDER_18_ÅR                                 |                  | 26.12.2007 | 25.12.2025 | OPPFYLT  | Nei                  |                      |                  |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER |                  | 01.02.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | UNDER_18_ÅR,GIFT_PARTNERSKAP                |                  | 26.12.2007 | 13.07.2024 | OPPFYLT  | Nei                  |                      |                  |
      | 1       | BOSATT_I_RIKET,BOR_MED_SØKER,LOVLIG_OPPHOLD |                  | 01.02.2023 | 13.07.2024 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 1       | 1            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 1       | 1            | 01.01.2024 | 30.11.2025 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |

      | 1       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 1       | 2            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 1       | 2            | 01.01.2024 | 31.07.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent følgende vedtaksperioder for behandling 2
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar |
      | 01.08.2024 |          | OPPHØR             |           |

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk | Gyldige begrunnelser | Ugyldige begrunnelser |
      | 01.08.2024 |          | OPPHØR             |           | OPPHØR_BARN_DØD      |                       |