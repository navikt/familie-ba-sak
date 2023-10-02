# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser ved endring av vilkår

  Bakgrunn:
    Gitt følgende behandling
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 13.04.2020  |

  Scenario: Skal lage vedtaksperioder for mor med et barn med vilkår
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 13.04.2020 | 10.03.2021 | Oppfylt  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2021 | 1354  | 1            |

    Og med vedtaksperioder for behandling 1
      | Fra dato   | Til dato | Standardbegrunnelser          | Eøsbegrunnelser | Fritekster |
      | 01.04.2021 |          | OPPHØR_BARN_FLYTTET_FRA_SØKER |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.04.2021 til -
      | Begrunnelse                   | Gjelder søker | Barnas fødselsdatoer | Antall barn med utbetaling | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | OPPHØR_BARN_FLYTTET_FRA_SØKER | Nei           | 13.04.20             | 1                          | mars 2021                            | NB      | 0     |                  | SØKER_HAR_IKKE_RETT     |

