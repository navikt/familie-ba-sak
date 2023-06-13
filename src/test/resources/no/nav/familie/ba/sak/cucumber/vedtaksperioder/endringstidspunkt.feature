# language: no
# encoding: UTF-8


Egenskap: Endringstidspunkt påvirker periodene

  Bakgrunn:
    Gitt følgende vedtak
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 24.12.1987  |
      | 1            | 3456    | BARN       | 01.02.2016  |


  Scenario: Skal kun ta med vedtaksperioder som kommer etter

    Og lag personresultater for behandling 1
    Og med overstyring av vilkår for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET                                   | 24.12.1987 |            | Oppfylt  |                      |
      | 1234    | LOVLIG_OPPHOLD                                   | 24.12.1987 | 01.12.2020 | Oppfylt  |                      |

      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 02.12.2016 |            | Oppfylt  |                      |
      | 3456    | BOR_MED_SØKER                                    | 02.12.2016 | 01.12.2020 | Oppfylt  |                      |
      | 3456    | UNDER_18_ÅR                                      | 02.12.2016 | 30.11.2034 | Oppfylt  |                      |

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår         | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | LOVLIG_OPPHOLD | 02.12.2020 | 30.09.2021 | ikke_oppfylt | Ja                   |
      | 1234    | LOVLIG_OPPHOLD | 01.10.2021 |            | Oppfylt      |                      |

      | 3456    | BOR_MED_SØKER  | 02.12.2020 | 30.09.2021 | ikke_oppfylt | Ja                   |
      | 3456    | BOR_MED_SØKER  | 01.10.2021 |            | Oppfylt      |                      |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.12.2016 | 31.12.2020 | 1234  | 1            |
      | 3456    | 01.10.2021 | 30.11.2034 | 1234  | 1            |

    Og med endringstidspunkt
      | Endringstidspunkt | BehandlingId |
      | 01.11.2021        | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar                                                     |
      | 01.01.2021 | 31.10.2021 | Avslag             | Avslag skal alltid med, selv om de er før endringstidspunktet |
      | 01.11.2021 | 30.11.2034 | Utbetaling         | Etter endringstidspunktet                                     |
      | 01.12.2034 |            | Opphør             | Barn er over 18                                               |