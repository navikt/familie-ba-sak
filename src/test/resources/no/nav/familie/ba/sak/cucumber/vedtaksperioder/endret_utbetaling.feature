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
      | 1            | 3456    | BARN       | 02.12.2016  |

  Scenario: Skal lage ikke utbetalingsperiode når andelene er endret til 0% og det ikke er delt bosted

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |                      |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 02.12.2016 |            | Oppfylt  |                      |
      | 3456    | UNDER_18_ÅR                                                     | 02.12.2016 | 01.12.2034 | Oppfylt  |                      |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId | Prosent |
      | 3456    | 01.01.2017 | 30.11.2034 | 1234  | 1            | 0       |

    Og med endrede utbetalinger
      | AktørId | Fra dato   | Til dato   | BehandlingId | Årsak             | Prosent |
      | 3456    | 01.01.2017 | 30.11.2034 | 1            | ETTERBETALING_3ÅR | 0       |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar            |
      | 01.01.2017 |          | Opphør             | Endret utbetaling 0% |

  Scenario:  Skal lage utbetalingsperiode når andelene er endret til 0% og det er delt bosted

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |                      |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 02.12.2016 |            | Oppfylt  |                      |
      | 3456    | UNDER_18_ÅR                                                     | 02.12.2016 | 01.12.2034 | Oppfylt  |                      |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId | Prosent |
      | 3456    | 01.01.2017 | 30.11.2034 | 1234  | 1            | 0       |

    Og med endrede utbetalinger
      | AktørId | Fra dato   | Til dato   | BehandlingId | Årsak       | prosent |
      | 3456    | 01.01.2017 | 30.11.2034 | 1            | DELT_BOSTED | 0       |


    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar       |
      | 01.01.2017 | 30.11.2034 | Utbetaling         | Delt bosted     |
      | 01.12.2034 |            | Opphør             | Barn er over 18 |
