# language: no
# encoding: UTF-8

Egenskap: Fagsaktype

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1000601  | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId |
      | 1000853      | 1000601  |                     |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId       | Persontype | Fødselsdato |
      | 1000853      | 2801053239878 | BARN       | 03.08.2017  |
      | 1000853      | 2204441081804 | SØKER      | 05.06.1988  |

  Scenario: Skal ikke gi institusjonsbegrunnelser når vi har normal fagsak
    Og lag personresultater for begrunnelse for behandling 1000853

    Og legg til nye vilkårresultater for begrunnelse for behandling 1000853
      | AktørId       | Vilkår                                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2801053239878 | UNDER_18_ÅR                                   |                  | 03.08.2017 | 02.08.2035 | OPPFYLT  | Nei                  |
      | 2801053239878 | GIFT_PARTNERSKAP,BOR_MED_SØKER,LOVLIG_OPPHOLD |                  | 03.08.2017 |            | OPPFYLT  | Nei                  |
      | 2801053239878 | BOSATT_I_RIKET                                |                  | 03.08.2017 | 15.03.2019 | OPPFYLT  | Nei                  |

      | 2204441081804 | LOVLIG_OPPHOLD,BOSATT_I_RIKET                 |                  | 05.06.1988 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 2801053239878 | 1000853      | 01.09.2017 | 28.02.2019 | 970   | ORDINÆR_BARNETRYGD | 100     |
      | 2801053239878 | 1000853      | 01.03.2019 | 31.03.2019 | 1054  | ORDINÆR_BARNETRYGD | 100     |

    Når begrunnelsetekster genereres for behandling 1000853

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser | Ekskluderte Begrunnelser          |
      | 01.09.2017 | 28.02.2019 | UTBETALING         |           |                         |                                   |
      | 01.03.2019 | 31.03.2019 | UTBETALING         |           |                         | INNVILGET_SATSENDRING_INSTITUSJON |
      | 01.04.2019 |            | OPPHØR             |           |                         |                                   |