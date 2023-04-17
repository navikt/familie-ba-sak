# language: no
# encoding: UTF-8

Egenskap: Vedtak med flere identer


  Scenario: Finn et bra navn

    Gitt følgende vedtak
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | Persontype |
      | 1            | SØKER      |
      | 1            | BARN       |

    Og med personresultater for behandling 1

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype |
      | 01.02.2022 | 30.04.2023 | Utbetaling         |
      | 01.05.2023 | 31.01.2040 | Utbetaling         |
      | 01.02.2040 |            | Utbetaling         |




