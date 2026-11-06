# language: no
# encoding: UTF-8

Egenskap: Vilkårsvurderingssteget rekjøres ikke når behandlingen er forbi steget

  Bakgrunn:
    Gitt følgende behandlinger
      | BehandlingId | Behandlingssteg    |
      | 1            | SEND_TIL_BESLUTTER |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 24.12.1987  |
      | 1            | 3456    | BARN       | 01.02.2016  |

  Scenario: Skal ikke generere tilkjent ytelse på nytt når vilkårsvurderingssteget kjøres på en behandling som er forbi steget

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 24.12.1987 |            | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 01.02.2016 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                                     | 01.02.2016 | 31.01.2034 | Oppfylt  |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.03.2016 | 31.01.2034 | 1054  | 1            |

    Når vi utfører vilkårsvurderingssteget for behandling 1

    Så forvent følgende andeler tilkjent ytelse for behandling 1
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.03.2016 | 31.01.2034 | 1054  | 1            |
