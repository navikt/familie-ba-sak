# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for avslag

  Bakgrunn:
    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId |
      | 1            | 1        |                     |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 03.01.1978  |
      | 1            | 2       | BARN       | 16.02.2007  |

  Scenario: Skal ikke krasje ved avslag uten fom- eller tomdato
    Og lag personresultater for behandling 1

    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 2       |

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                            | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD                    |                  |            |            | IKKE_OPPFYLT | Ja                   |
      | 1       | UTVIDET_BARNETRYGD,BOSATT_I_RIKET |                  | 22.09.2022 |            | OPPFYLT      | Nei                  |

      | 2       | LOVLIG_OPPHOLD                    |                  |            |            | IKKE_OPPFYLT | Ja                   |
      | 2       | GIFT_PARTNERSKAP                  |                  | 16.02.2007 |            | OPPFYLT      | Nei                  |
      | 2       | UNDER_18_ÅR                       |                  | 16.02.2007 | 15.02.2025 | OPPFYLT      | Nei                  |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET      |                  | 22.09.2022 |            | OPPFYLT      | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato | Til dato | Beløp | Ytelse type | Prosent |

    Og med endrede utbetalinger
      | AktørId | Fra dato | Til dato | BehandlingId | Årsak | Prosent |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato | Til dato | VedtaksperiodeType | Regelverk | Gyldige begrunnelser | Ugyldige begrunnelser |
      |          |          | AVSLAG             |           |                         |                          |
