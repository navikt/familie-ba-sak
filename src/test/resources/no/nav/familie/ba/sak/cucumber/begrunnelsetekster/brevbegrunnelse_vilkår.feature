# language: no
# encoding: UTF-8

Egenskap: Begrunnelser ved endring av vilkår

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

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser       | Ekskluderte Begrunnelser |
      | 01.05.2020 | 31.03.2021 | UTBETALING         |                               |                          |
      | 01.04.2021 |            | OPPHØR             | OPPHØR_BARN_FLYTTET_FRA_SØKER |                          |

  Scenario: Søker får lovlig opphold etter barn
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 15.01.2021 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 13.04.2020 | 10.03.2021 | Oppfylt  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.02.2021 | 31.03.2021 | 1354  | 1            |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser                  | Ekskluderte Begrunnelser |
      | 01.02.2021 | 31.03.2021 | UTBETALING         | INNVILGET_BOSATT_I_RIKTET_LOVLIG_OPPHOLD |                          |
      | 01.04.2021 |            | OPPHØR             | OPPHØR_BARN_FLYTTET_FRA_SØKER            |                          |

  Scenario: Bor med søker for barn er eneste utgjørende vilkår skal kun gi bor med søker begrunnelser
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 15.01.1990 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 13.04.2021 | 10.03.2022 | Oppfylt  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2021 | 31.03.2022 | 1354  | 1            |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser       | Ekskluderte Begrunnelser      |
      | 01.05.2021 | 31.03.2022 | UTBETALING         | INNVILGET_BOR_HOS_SØKER       | REDUKSJON_IKKE_BOSATT_I_NORGE |
      | 01.04.2022 |            | OPPHØR             | OPPHØR_BARN_FLYTTET_FRA_SØKER |                               |
    
  Scenario: Skal ikke gi redukjsonsbegrunnelse når det er innvilgelse
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 15.01.1990 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 13.04.2021 | 10.03.2022 | Oppfylt  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2021 | 31.03.2022 | 1354  | 1            |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Inkluderte Begrunnelser       | Ekskluderte Begrunnelser |
      | 01.05.2021 | 31.03.2022 | UTBETALING         | INNVILGET_BOR_HOS_SØKER       | REDUKSJON_FLYTTET_BARN   |
      | 01.04.2022 |            | OPPHØR             | OPPHØR_BARN_FLYTTET_FRA_SØKER |                          |