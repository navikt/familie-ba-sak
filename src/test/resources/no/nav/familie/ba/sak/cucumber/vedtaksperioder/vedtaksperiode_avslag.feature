# language: no
# encoding: UTF-8


Egenskap: Vedtaksperioder med mor og to barn

  Bakgrunn:
    Gitt følgende vedtak
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 24.12.1987  |
      | 1            | 3456    | BARN       | 01.02.2016  |


  Scenario: Skal kun lage én avslagsperiode når det er avslag på søker hele perioden og ingen andre avslag

    Og lag personresultater for behandling 1
    Og med overstyring av vilkår for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET                                                  | 24.12.1987 |            | Oppfylt      |                      |
      | 1234    | LOVLIG_OPPHOLD                                                  | 24.12.1987 |            | ikke_oppfylt | Ja                   |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 01.12.2016 |            | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                                                     | 01.12.2016 | 30.11.2034 | Oppfylt      |                      |
      | 3456    | BOR_MED_SØKER, GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 07.12.2022 |            | Oppfylt      |                      |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar |
      | 1988-01-01 |          | AVSLAG             | Kun søker |


  Scenario: Skal kun lage én avslagsperiode når det er avslag på søker hele perioden og ingen andre avslag. Ingen startdato

    Og lag personresultater for behandling 1
    Og med overstyring av vilkår for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET                                                  | 24.12.1987 |            | Oppfylt      |                      |
      | 1234    | LOVLIG_OPPHOLD                                                  |            |            | ikke_oppfylt | Ja                   |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 01.12.2016 |            | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                                                     | 01.12.2016 | 30.11.2034 | Oppfylt      |                      |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato | Til dato | Vedtaksperiodetype | Kommentar |
      |          |          | AVSLAG             | Kun søker |

  Scenario: Skal lage tre avslagsperioder når søker har konstant avslag og barn har en avslagsperiode med fom og tom

    Og lag personresultater for behandling 1
    Og med overstyring av vilkår for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET                                   | 24.12.1987 |            | Oppfylt      |                      |
      | 1234    | LOVLIG_OPPHOLD                                   |            |            | ikke_oppfylt | Ja                   |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 01.12.2016 |            | Oppfylt      |                      |
      | 3456    | BOR_MED_SØKER                                    | 01.12.2016 | 01.12.2020 | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                                      | 01.12.2016 | 30.11.2034 | Oppfylt      |                      |

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår        | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 3456    | BOR_MED_SØKER | 02.12.2020 | 30.09.2021 | ikke_oppfylt | Ja                   |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar                     |
      |            | 31.12.2020 | AVSLAG             | Kun søker har avslag          |
      | 01.01.2021 | 30.09.2021 | AVSLAG             | Både søker og barn har avslag |
      | 01.10.2021 |            | AVSLAG             | Kun søker har avslag          |


  Scenario: Skal lage 5 avslagsperioder når søker har konstant avslag og barn har vilkår med overlappende avslag

    Og lag personresultater for behandling 1
    Og med overstyring av vilkår for behandling 1
      | AktørId | Vilkår                           | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET                   | 24.12.1987 |            | Oppfylt      |                      |
      | 1234    | LOVLIG_OPPHOLD                   |            |            | ikke_oppfylt | Ja                   |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET | 01.12.2016 |            | Oppfylt      |                      |
      | 3456    | LOVLIG_OPPHOLD                   | 01.12.2016 | 30.05.2020 | Oppfylt      |                      |
      | 3456    | BOR_MED_SØKER                    | 01.12.2016 | 01.12.2020 | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                      | 01.12.2016 | 30.11.2034 | Oppfylt      |                      |

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår         | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 3456    | LOVLIG_OPPHOLD | 08.06.2020 | 30.09.2021 | ikke_oppfylt | Ja                   |
      | 3456    | BOR_MED_SØKER  | 02.12.2020 | 31.12.2021 | ikke_oppfylt | Ja                   |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar                                                        |
      |            | 30.06.2020 | AVSLAG             | Kun søker har avslag                                             |
      | 01.07.2020 | 31.12.2020 | AVSLAG             | Både søker og barn har avslag på BOR_MED_SØKER                   |
      | 01.01.2021 | 30.09.2021 | AVSLAG             | Både søker og barn har avslag på LOVLIG_OPPHOLD og BOR_MED_SØKER |
      | 01.10.2021 | 31.12.2021 | AVSLAG             | Både søker og barn har avslag på LOVLIG_OPPHOLD                  |
      | 01.01.2022 |            | AVSLAG             | Kun søker har avslag                                             |



