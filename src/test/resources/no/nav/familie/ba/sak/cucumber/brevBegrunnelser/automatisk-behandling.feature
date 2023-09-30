# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser ved automatiske behandlinger

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk |
      | 1            | 1        |                     | INNVILGET           | FØDSELSHENDELSE  | Ja                        |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 12.08.1995  |
      | 1            | 2       | BARN       | 04.06.2023  |

  Scenario: Skal sette riktige felter for automatisk fødselshendelse
    Og følgende dagens dato 05.06.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2       | GIFT_PARTNERSKAP,BOSATT_I_RIKET,LOVLIG_OPPHOLD,BOR_MED_SØKER |                  | 04.06.2023 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                                  |                  | 04.06.2023 | 03.06.2041 | OPPFYLT  | Nei                  |

      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET                                |                  | 12.08.1995 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.07.2023 | 31.05.2029 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.06.2029 | 31.05.2041 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Når begrunnelsetekster genereres for behandling 1

    Og med vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                         | Eøsbegrunnelser | Fritekster |
      | 01.07.2023 | 31.05.2029 | INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.07.2023 til 31.05.2029
      | Begrunnelse                                  | Gjelder søker | Barnas fødselsdager | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE | Nei           | 04.06.23            | 1           | juni 2023                            | NB      | 1766  |                  | SØKER_HAR_IKKE_RETT     |