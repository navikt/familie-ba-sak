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


  Scenario: Skal lage en avslagsperiode når det er avslag

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
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 2017-01-01 | 2034-11-30 | AVSLAG             | Kun søker |
      | 2034-12-01 |            | AVSLAG             | Kun søker |





