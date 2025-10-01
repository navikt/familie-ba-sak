# language: no
# encoding: UTF-8

Egenskap: Behandlig med svalbardtillegg

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status    |
      | 1        | NORMAL     | OPPRETTET |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | Behandlingsresultat | Behandlingsårsak | Behandlingsstatus |
      | 1            | 1        | DELVIS_INNVILGET    | SØKNAD           | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 31.03.1990  |              |
      | 1            | 2       | BARN       | 08.11.2023  |              |

  Scenario: Skal ikke splitte vedtaksperioder når søker flytter til svalbard men ikke barn
    Og dagens dato er 05.09.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 2       |
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår             | Fra dato   | Til dato   | Resultat |
      | 1       | LOVLIG_OPPHOLD                              |                              | 31.03.1990 |            | OPPFYLT  |
      | 1       | BOSATT_I_RIKET                              |                              | 08.11.2018 | 06.08.2025 | OPPFYLT  |
      | 1       | BOSATT_I_RIKET                              | BOSATT_PÅ_SVALBARD | 07.08.2025 |            | OPPFYLT  |

      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                              | 08.11.2023 |            | OPPFYLT  |
      | 2       | GIFT_PARTNERSKAP                            |                              | 08.11.2023 |            | OPPFYLT  |
      | 2       | UNDER_18_ÅR                                 |                              | 08.11.2023 | 07.11.2036 | OPPFYLT  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.12.2023 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 1            | 01.06.2025 | 31.10.2036 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |


    Når vedtaksperiodene genereres for behandling 1


    Så forvent følgende vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.12.2023 | 31.05.2025 | UTBETALING         |           |
      | 01.06.2025 | 31.10.2036 | UTBETALING         |           |
      | 01.11.2036 |            | OPPHØR             |           |


  Scenario: Skal lage splitt i vedtaksperioder når det er andre utdypende vilkår enn svalbardtillegg
    Og dagens dato er 05.09.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 2       |
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår                                 | Fra dato   | Til dato   | Resultat |
      | 1       | LOVLIG_OPPHOLD                              |                                                  | 31.03.1990 |            | OPPFYLT  |
      | 1       | BOSATT_I_RIKET                              |                                                  | 08.11.2018 | 06.08.2025 | OPPFYLT  |
      | 1       | BOSATT_I_RIKET                              | VURDERT_MEDLEMSKAP, BOSATT_PÅ_SVALBARD | 07.08.2025 |            | OPPFYLT  |

      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                                                  | 08.11.2018 |            | OPPFYLT  |
      | 2       | GIFT_PARTNERSKAP                            |                                                  | 08.11.2018 |            | OPPFYLT  |
      | 2       | UNDER_18_ÅR                                 |                                                  | 08.11.2018 | 07.11.2036 | OPPFYLT  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.12.2023 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 1            | 01.06.2025 | 31.10.2036 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent følgende vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.12.2023 | 31.05.2025 | UTBETALING         |           |
      | 01.06.2025 | 31.08.2025 | UTBETALING         |           |
      | 01.09.2025 | 31.10.2036 | UTBETALING         |           |
      | 01.11.2036 |            | OPPHØR             |           |


  Scenario: Skal lage splitt i vedtaksperioder når det er endring i regelverk og ikke svalbardtillegg
    Og dagens dato er 05.09.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 2       |
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD                              |                              | 31.03.1990 |            | OPPFYLT  |                  |
      | 1       | BOSATT_I_RIKET                              |                              | 08.11.2018 | 06.08.2025 | OPPFYLT  |                  |
      | 1       | BOSATT_I_RIKET                              | BOSATT_PÅ_SVALBARD | 07.08.2025 |            | OPPFYLT  | EØS_FORORDNINGEN |

      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                              | 08.11.2018 |            | OPPFYLT  |                  |
      | 2       | GIFT_PARTNERSKAP                            |                              | 08.11.2018 |            | OPPFYLT  |                  |
      | 2       | UNDER_18_ÅR                                 |                              | 08.11.2018 | 07.11.2036 | OPPFYLT  |                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.12.2023 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 1            | 01.06.2025 | 31.10.2036 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent følgende vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.12.2023 | 31.05.2025 | UTBETALING         |           |
      | 01.06.2025 | 31.08.2025 | UTBETALING         |           |
      | 01.09.2025 | 31.10.2036 | UTBETALING         |           |
      | 01.11.2036 |            | OPPHØR             |           |


  Scenario: Skal splitte vedtaksperioder når søker flytter ut av svalbard men ikke barn
    Og dagens dato er 05.09.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 2       |
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår             | Fra dato   | Til dato   | Resultat |
      | 1       | LOVLIG_OPPHOLD                              |                              | 31.03.1990 |            | OPPFYLT  |
      | 1       | BOSATT_I_RIKET                              | BOSATT_PÅ_SVALBARD | 08.11.2018 | 06.08.2025 | OPPFYLT  |
      | 1       | BOSATT_I_RIKET                              |                              | 07.08.2025 |            | OPPFYLT  |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                              | 08.11.2018 |            | OPPFYLT  |
      | 2       | BOSATT_I_RIKET                              | BOSATT_PÅ_SVALBARD | 08.11.2018 |            | OPPFYLT  |
      | 2       | GIFT_PARTNERSKAP                            |                              | 08.11.2018 |            | OPPFYLT  |
      | 2       | UNDER_18_ÅR                                 |                              | 08.11.2018 | 07.11.2036 | OPPFYLT  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.12.2023 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 1            | 01.06.2025 | 31.10.2036 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |

    Når vedtaksperiodene genereres for behandling 1


    Så forvent følgende vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.12.2023 | 31.05.2025 | UTBETALING         |           |
      | 01.06.2025 | 31.10.2036 | UTBETALING         |           |
      | 01.11.2036 |            | OPPHØR             |           |

  Scenario: Skal ikke splitte vedtaksperioder når søker flytter ut av svalbard i samme måned som barn flytter inn
    Og dagens dato er 05.09.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 2       |
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår             | Fra dato   | Til dato   | Resultat |
      | 1       | LOVLIG_OPPHOLD                |                              | 31.03.1990 |            | OPPFYLT  |
      | 1       | BOSATT_I_RIKET                |                              | 08.11.2018 | 06.08.2025 | OPPFYLT  |
      | 1       | BOSATT_I_RIKET                | BOSATT_PÅ_SVALBARD | 31.08.2025 |            | OPPFYLT  |
      | 2       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                              | 08.11.2018 |            | OPPFYLT  |
      | 2       | BOR_MED_SØKER                 | BOSATT_PÅ_SVALBARD | 08.11.2018 | 01.08.2025 | OPPFYLT  |
      | 2       | GIFT_PARTNERSKAP              |                              | 08.11.2018 |            | OPPFYLT  |
      | 2       | UNDER_18_ÅR                   |                              | 08.11.2018 | 07.11.2036 | OPPFYLT  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.12.2023 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 1            | 01.06.2025 | 31.10.2036 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |


    Når vedtaksperiodene genereres for behandling 1


    Så forvent følgende vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar |
      | 01.12.2023 | 31.05.2025 | UTBETALING         |           |
      | 01.06.2025 | 31.10.2036 | UTBETALING         |           |
      | 01.11.2036 |            | OPPHØR             |           |