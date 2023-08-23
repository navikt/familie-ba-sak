# language: no
# encoding: UTF-8

Egenskap: Kompetanse-begrunnelser

  Bakgrunn:
    Gitt følgende behandling
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 13.04.2020  |

    #TODO: Legg til forventede inkluderte begrunnelser
  Scenario: Skal gi innvilget primærland begrunnelse basert på kompetanse
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 30.04.2021 | 1054  | 1            |
      | 3456    | 01.05.2021 | 31.03.2038 | 1354  | 1            |

    Og med kompetanser for begrunnelse
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Annen forelders aktivitet | Barnets bostedsland |
      | 3456    | 01.05.2020 | 30.04.2021 | NORGE_ER_PRIMÆRLAND   | 1            | IKKE_AKTUELT              | NORGE               |
      | 3456    | 01.05.2021 | 31.03.2038 | NORGE_ER_SEKUNDÆRLAND | 1            |                           |                     |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk        | Inkluderte Begrunnelser | Ekskluderte Begrunnelser                           |
      | 01.05.2020 | 30.04.2021 | Utbetaling         | EØS_FORORDNINGEN |                         | INNVILGET_PRIMÆRLAND_BEGGE_FORELDRE_JOBBER_I_NORGE |
      | 01.05.2021 | 31.03.2038 | Utbetaling         |                  |                         |                                                    |
      | 01.04.2038 |            | Opphør             |                  |                         |                                                    |

