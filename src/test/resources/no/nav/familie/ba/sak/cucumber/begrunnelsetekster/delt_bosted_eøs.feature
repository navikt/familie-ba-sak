# language: no
# encoding: UTF-8

Egenskap: Gyldige begrunnelser for delt bosted i EØS-saker

  Bakgrunn:
    Gitt følgende behandling
      | BehandlingId |
      | 1            |

  Scenario: Skal få begrunnelser for delt bosted skal deles i EØS-sak
    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 13.04.2020  |
      | 1            | 7890    | BARN       | 13.04.2020  |
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat | Utdypende vilkår | Vurderes etter |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 11.01.1970 |            | Oppfylt  |                  |                |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |                  |                |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |                  |                |
      | 3456    | BOR_MED_SØKER                                    | 13.04.2020 | 10.03.2021 | Oppfylt  | DELT_BOSTED      |                |
      | 7890    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |                  |                |
      | 7890    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |                  |                |
      | 7890    | BOR_MED_SØKER                                    | 13.04.2020 | 10.04.2021 | Oppfylt  | DELT_BOSTED      |                |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2021 | 1354  | 1            |
      | 7890    | 01.05.2020 | 30.04.2021 | 1354  | 1            |

    Og med kompetanser for begrunnelse
      | AktørId | Fra dato   | Til dato   | Resultat            | BehandlingId | Annen forelders aktivitet | Barnets bostedsland |
      | 3456    | 01.05.2020 | 31.03.2021 | NORGE_ER_PRIMÆRLAND | 1            | IKKE_AKTUELT              | NO                  |
      | 7890    | 01.05.2020 | 30.04.2021 | NORGE_ER_PRIMÆRLAND | 1            | IKKE_AKTUELT              | NO                  |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                             | Ugyldige begrunnelser |
      | 01.05.2020 | 31.03.2021 | UTBETALING         | EØS_FORORDNINGEN               | INNVILGET_TILLEGGSTEKST_DELT_BOSTED                              |                       |
      | 01.04.2021 | 30.04.2021 | UTBETALING         | EØS_FORORDNINGEN               | REDUKSJON_DELT_BOSTED_BEGGE_FORELDRE_IKKE_OMFATTET_NORSK_LOVVALG |                       |
      | 01.05.2021 |            | OPPHØR             | EØS_FORORDNINGEN               | OPPHØR_DELT_BOSTED_BEGGE_FORELDRE_IKKE_OMFATTET_NORSK_LOVGIVNING |                       |

  Scenario: Skal få begrunnelser for delt bosted skal ikke deles i EØS-sak
    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 13.04.2020  |

    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat | Utdypende vilkår            | Vurderes etter |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 11.01.1970 |            | Oppfylt  |                             |                |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |                             |                |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |                             |                |
      | 3456    | BOR_MED_SØKER                                    | 13.04.2020 | 10.03.2021 | Oppfylt  | DELT_BOSTED_SKAL_IKKE_DELES |                |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2021 | 1354  | 1            |


    Og med kompetanser for begrunnelse
      | AktørId | Fra dato   | Til dato   | Resultat            | BehandlingId | Annen forelders aktivitet | Barnets bostedsland |
      | 3456    | 01.05.2020 | 31.03.2021 | NORGE_ER_PRIMÆRLAND | 1            | IKKE_AKTUELT              | NO                  |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                    | Ugyldige begrunnelser |
      | 01.05.2020 | 31.03.2021 | UTBETALING         | EØS_FORORDNINGEN               | INNVILGET_TILLEGGSTEKST_FULL_BARNETRYGD_HAR_AVTALE_DELT |                       |
      | 01.04.2021 |            | OPPHØR             |                                |                                                         |                       |

