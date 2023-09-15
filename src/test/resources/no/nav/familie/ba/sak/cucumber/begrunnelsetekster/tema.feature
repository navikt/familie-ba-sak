# language: no
# encoding: UTF-8

Egenskap: Tema

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId |
      | 1            | 1        |                     |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 02.01.1985  |
      | 1            | 4567    | BARN       | 07.09.2019  |

  Scenario: Man skal ikke få nasjonale begrunnelser dersom vedtaksperiode overlapper med eøs perioder
    Og følgende dagens dato 2023-09-13
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                          | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | LOVLIG_OPPHOLD                  |                              | 02.01.1985 |            | OPPFYLT  | Nei                  |
      | 1234    | BOSATT_I_RIKET                  | OMFATTET_AV_NORSK_LOVGIVNING | 02.01.1985 |            | OPPFYLT  | Nei                  |

      | 4567    | UNDER_18_ÅR                     |                              | 07.09.2019 | 06.09.2037 | OPPFYLT  | Nei                  |
      | 4567    | LOVLIG_OPPHOLD,GIFT_PARTNERSKAP |                              | 07.09.2019 |            | OPPFYLT  | Nei                  |
      | 4567    | BOSATT_I_RIKET                  | BARN_BOR_I_NORGE             | 07.09.2019 |            | OPPFYLT  | Nei                  |
      | 4567    | BOR_MED_SØKER                   | BARN_BOR_I_EØS_MED_SØKER     | 07.09.2019 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 4567    | 1            | 01.10.2019 | 31.08.2020 | 1054  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.09.2020 | 31.08.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.09.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.01.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.07.2023 | 31.08.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.09.2025 | 31.08.2037 | 1310  | ORDINÆR_BARNETRYGD | 100     |

    Og med kompetanser for begrunnelse
      | AktørId | Fra dato   | Til dato | Resultat            | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 4567    | 01.10.2019 |          | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | SE                             | SE                  |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Inkluderte Begrunnelser | Inkluderte Begrunnelser                           | Regelverk Ekskluderte Begrunnelser | Ekskluderte Begrunnelser     |
      | 01.10.2019 | 31.08.2020 | UTBETALING         | EØS_FORORDNINGEN                  | INNVILGET_PRIMÆRLAND_BARNETRYGD_ALLEREDE_UTBETALT | NASJONALE_REGLER                   | INNVILGET_NYFØDT_BARN_FØRSTE |
      | 01.09.2020 | 31.08.2021 | UTBETALING         |                                   |                                                   |                                    |                              |
      | 01.09.2021 | 31.12.2021 | UTBETALING         |                                   |                                                   |                                    |                              |
      | 01.01.2022 | 28.02.2023 | UTBETALING         |                                   |                                                   |                                    |                              |
      | 01.03.2023 | 30.06.2023 | UTBETALING         |                                   |                                                   |                                    |                              |
      | 01.07.2023 | 31.08.2025 | UTBETALING         |                                   |                                                   |                                    |                              |
      | 01.09.2025 | 31.08.2037 | UTBETALING         |                                   |                                                   |                                    |                              |
      | 01.09.2037 |            | OPPHØR             |                                   |                                                   |                                    |                              |

  Scenario: Man skal ikke få eøs begrunnelser dersom vedtaksperiode ikke overlapper med nasjonale perioder
    Og følgende dagens dato 2023-09-13
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                          | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | LOVLIG_OPPHOLD                  |                              | 02.01.1985 |            | OPPFYLT  | Nei                  |
      | 1234    | BOSATT_I_RIKET                  | OMFATTET_AV_NORSK_LOVGIVNING | 02.01.1985 |            | OPPFYLT  | Nei                  |

      | 4567    | UNDER_18_ÅR                     |                              | 07.09.2019 | 06.09.2037 | OPPFYLT  | Nei                  |
      | 4567    | LOVLIG_OPPHOLD,GIFT_PARTNERSKAP |                              | 07.09.2019 |            | OPPFYLT  | Nei                  |
      | 4567    | BOSATT_I_RIKET                  | BARN_BOR_I_NORGE             | 07.09.2019 |            | OPPFYLT  | Nei                  |
      | 4567    | BOR_MED_SØKER                   | BARN_BOR_I_EØS_MED_SØKER     | 07.09.2019 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 4567    | 1            | 01.10.2019 | 31.08.2020 | 1054  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.09.2020 | 31.08.2021 | 1354  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.09.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.01.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.07.2023 | 31.08.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     |
      | 4567    | 1            | 01.09.2025 | 31.08.2037 | 1310  | ORDINÆR_BARNETRYGD | 100     |

    Når begrunnelsetekster genereres for behandling 1

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Inkluderte Begrunnelser | Inkluderte Begrunnelser                  | Regelverk Ekskluderte Begrunnelser | Ekskluderte Begrunnelser                          |
      | 01.10.2019 | 31.08.2020 | UTBETALING         | NASJONALE_REGLER                  | INNVILGET_BOSATT_I_RIKTET_LOVLIG_OPPHOLD | EØS_FORORDNINGEN                   | INNVILGET_PRIMÆRLAND_BARNETRYGD_ALLEREDE_UTBETALT |
      | 01.09.2020 | 31.08.2021 | UTBETALING         |                                   |                                          |                                    |                                                   |
      | 01.09.2021 | 31.12.2021 | UTBETALING         |                                   |                                          |                                    |                                                   |
      | 01.01.2022 | 28.02.2023 | UTBETALING         |                                   |                                          |                                    |                                                   |
      | 01.03.2023 | 30.06.2023 | UTBETALING         |                                   |                                          |                                    |                                                   |
      | 01.07.2023 | 31.08.2025 | UTBETALING         |                                   |                                          |                                    |                                                   |
      | 01.09.2025 | 31.08.2037 | UTBETALING         |                                   |                                          |                                    |                                                   |
      | 01.09.2037 |            | OPPHØR             |                                   |                                          |                                    |                                                   |