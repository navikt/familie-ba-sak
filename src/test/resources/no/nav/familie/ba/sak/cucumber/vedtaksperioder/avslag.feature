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
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET                                                  | 24.12.1987 |            | Oppfylt      |                      |
      | 1234    | LOVLIG_OPPHOLD                                                  | 24.12.1987 |            | ikke_oppfylt | Ja                   |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 01.12.2016 |            | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                                                     | 01.12.2016 | 30.11.2034 | Oppfylt      |                      |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar |
      | 1988-01-01 |          | AVSLAG             | Kun søker |


  Scenario: Skal kun lage én avslagsperiode når det er avslag på søker hele perioden og ingen andre avslag. Ingen startdato

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET                                                  | 24.12.1987 |            | Oppfylt      |                      |
      | 1234    | LOVLIG_OPPHOLD                                                  |            |            | ikke_oppfylt | Ja                   |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 01.12.2016 |            | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                                                     | 01.12.2016 | 30.11.2034 | Oppfylt      |                      |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato | Til dato | Vedtaksperiodetype | Kommentar |
      |          |          | AVSLAG             | Kun søker |

  Scenario: Skal lage to avslagsperioder når søker har konstant avslag og barn har en avslagsperiode med fom og tom

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET                                   | 24.12.1987 |            | Oppfylt      |                      |
      | 1234    | LOVLIG_OPPHOLD                                   |            |            | ikke_oppfylt | Ja                   |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 01.12.2016 |            | Oppfylt      |                      |
      | 3456    | BOR_MED_SØKER                                    | 01.12.2016 | 01.12.2020 | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                                      | 01.12.2016 | 30.11.2034 | Oppfylt      |                      |
      | 3456    | BOR_MED_SØKER                                    | 02.12.2020 | 30.09.2021 | ikke_oppfylt | Ja                   |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar            |
      |            |            | AVSLAG             | Kun søker har avslag |
      | 01.01.2021 | 30.09.2021 | AVSLAG             | Barn har avslag      |


  Scenario: Skal lage avslagsperioder når søker har konstant avslag og barn har vilkår med overlappende avslag

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 24.12.1987  |
      | 1            | 3456    | BARN       | 01.12.2016  |
      | 1            | 5678    | BARN       | 01.02.2017  |

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                           | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET                   | 24.12.1987 |            | Oppfylt      |                      |
      | 1234    | LOVLIG_OPPHOLD                   |            |            | ikke_oppfylt | Ja                   |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET | 01.12.2016 |            | Oppfylt      |                      |
      | 3456    | LOVLIG_OPPHOLD                   | 01.12.2016 | 30.05.2020 | Oppfylt      |                      |
      | 3456    | BOR_MED_SØKER                    | 01.12.2016 | 01.12.2020 | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                      | 01.12.2016 | 30.11.2034 | Oppfylt      |                      |
      | 5678    | GIFT_PARTNERSKAP, BOSATT_I_RIKET | 01.12.2017 |            | Oppfylt      |                      |
      | 5678    | LOVLIG_OPPHOLD                   | 01.12.2017 | 30.05.2021 | Oppfylt      |                      |
      | 5678    | BOR_MED_SØKER                    | 01.12.2017 | 01.12.2021 | Oppfylt      |                      |
      | 5678    | UNDER_18_ÅR                      | 01.12.2017 | 30.11.2035 | Oppfylt      |                      |

      | 3456    | LOVLIG_OPPHOLD                   | 08.06.2020 | 30.09.2021 | ikke_oppfylt | Ja                   |
      | 3456    | BOR_MED_SØKER                    | 02.12.2020 | 31.12.2021 | ikke_oppfylt | Ja                   |

      | 5678    | LOVLIG_OPPHOLD                   | 08.06.2021 | 30.09.2022 | ikke_oppfylt | Ja                   |
      | 5678    | BOR_MED_SØKER                    | 02.12.2021 | 31.12.2022 | ikke_oppfylt | Ja                   |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar                          |
      |            |            | AVSLAG             | Søker har avslag                   |

      | 01.07.2020 | 31.12.2020 | AVSLAG             | Barn1 har avslag på LOVLIG_OPPHOLD |
      | 01.01.2021 | 30.09.2021 | AVSLAG             | Barn1 har avslag på to vilkår      |
      | 01.10.2021 | 31.12.2021 | AVSLAG             | Barn1 har avslag på BOR_MED_SØKER  |

      | 01.07.2021 | 31.12.2021 | AVSLAG             | Barn2 har avslag på LOVLIG_OPPHOLD |
      | 01.01.2022 | 30.09.2022 | AVSLAG             | Barn2 har avslag på to vilkår      |
      | 01.10.2022 | 31.12.2022 | AVSLAG             | Barn2 har avslag på BOR_MED_SØKER  |


  Scenario: Skal lage avslagsperiode og ekstra opphørsperiode når søker og barn 1 har eksplisitt avslag

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 24.12.1987  |
      | 1            | 3456    | BARN       | 02.12.2016  |
      | 1            | 5678    | BARN       | 02.12.2016  |

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET                                   | 24.12.1987 |            | Oppfylt      |                      |
      | 1234    | LOVLIG_OPPHOLD                                   | 24.12.1987 | 01.12.2020 | Oppfylt      |                      |

      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 02.12.2016 |            | Oppfylt      |                      |
      | 3456    | BOR_MED_SØKER                                    | 02.12.2016 | 01.12.2020 | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                                      | 02.12.2016 | 30.11.2034 | Oppfylt      |                      |

      | 5678    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 02.12.2016 |            | Oppfylt      |                      |
      | 5678    | BOR_MED_SØKER                                    | 02.12.2016 | 01.12.2020 | Oppfylt      |                      |
      | 5678    | UNDER_18_ÅR                                      | 02.12.2016 | 30.11.2034 | Oppfylt      |                      |

      | 1234    | LOVLIG_OPPHOLD                                   | 02.12.2020 | 30.09.2021 | ikke_oppfylt | Ja                   |
      | 1234    | LOVLIG_OPPHOLD                                   | 01.10.2021 |            | Oppfylt      |                      |

      | 3456    | BOR_MED_SØKER                                    | 02.12.2020 | 30.09.2021 | ikke_oppfylt | Ja                   |
      | 3456    | BOR_MED_SØKER                                    | 01.10.2021 |            | Oppfylt      |                      |

      | 5678    | BOR_MED_SØKER                                    | 02.12.2020 | 30.09.2021 | ikke_oppfylt |                      |
      | 5678    | BOR_MED_SØKER                                    | 01.10.2021 |            | Oppfylt      |                      |


    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.01.2017 | 31.12.2020 | 1234  | 1            |
      | 3456    | 01.11.2021 | 30.11.2034 | 1234  | 1            |
      | 5678    | 01.01.2017 | 31.12.2020 | 1234  | 1            |
      | 5678    | 01.11.2021 | 30.11.2034 | 1234  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar                                                                                             |
      | 01.01.2017 | 31.12.2020 | Utbetaling         |                                                                                                       |
      | 01.01.2021 | 31.09.2021 | Avslag             | Søker har opphør som overlapper med avslag hos barn                                                   |
      | 01.10.2021 | 31.10.2021 | Opphør             | Fortsatt opphør for barn 5678, denne perioden skal ikke begrunnes (trenger egentlig ikke å genereres) |
      | 01.11.2021 | 30.11.2034 | Utbetaling         |                                                                                                       |
      | 01.12.2034 |            | Opphør             | Barn er over 18                                                                                       |


  Scenario: Skal lage separate opphør- og avslagsperioder når barn 1 har eksplisitt avslag og barn 2 har ingen utbetaling

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 24.12.1987  |
      | 1            | 3456    | BARN       | 02.12.2016  |
      | 1            | 5678    | BARN       | 02.12.2016  |

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET                                   | 24.12.1987 |            | Oppfylt      |                      |
      | 1234    | LOVLIG_OPPHOLD                                   | 24.12.1987 |            | Oppfylt      |                      |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 02.12.2016 |            | Oppfylt      |                      |
      | 3456    | BOR_MED_SØKER                                    | 02.12.2016 | 01.12.2020 | Oppfylt      |                      |
      | 3456    | UNDER_18_ÅR                                      | 02.12.2016 | 30.11.2034 | Oppfylt      |                      |
      | 5678    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 02.12.2016 |            | Oppfylt      |                      |
      | 5678    | BOR_MED_SØKER                                    | 02.12.2016 | 01.12.2020 | Oppfylt      |                      |
      | 5678    | UNDER_18_ÅR                                      | 02.12.2016 | 30.11.2034 | Oppfylt      |                      |

      | 3456    | BOR_MED_SØKER                                    | 02.12.2020 | 30.09.2021 | ikke_oppfylt | Ja                   |
      | 3456    | BOR_MED_SØKER                                    | 01.10.2021 |            | Oppfylt      |                      |
      | 5678    | BOR_MED_SØKER                                    | 02.12.2020 | 30.09.2021 | ikke_oppfylt |                      |
      | 5678    | BOR_MED_SØKER                                    | 01.10.2021 |            | Oppfylt      |                      |


    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.01.2017 | 31.12.2020 | 1234  | 1            |
      | 3456    | 01.11.2021 | 30.11.2034 | 1234  | 1            |
      | 5678    | 01.01.2017 | 31.12.2020 | 1234  | 1            |
      | 5678    | 01.11.2021 | 30.11.2034 | 1234  | 1            |

    Når vedtaksperioder med begrunnelser genereres for behandling 1
    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar         |
      | 01.01.2017 | 31.12.2020 | Utbetaling         |                   |
      | 01.01.2021 | 30.09.2021 | Avslag             | Barn 1 har avslag |
      | 01.01.2021 | 31.10.2021 | Opphør             | Barn 2 har opphør |
      | 01.11.2021 | 30.11.2034 | Utbetaling         |                   |
      | 01.12.2034 |            | Opphør             | Barna er over 18  |

  Scenario: Skal kun lage avslagsperiode for eksplisitte avslåtte perioder for personer fremstilt krav for
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende vedtak
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat       | Behandlingsårsak |
      | 1            | 1        |                     | FORTSATT_INNVILGET        | SATSENDRING      |
      | 2            | 1        | 1                   | AVSLÅTT_ENDRET_OG_OPPHØRT | SØKNAD           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 15.10.1988  |
      | 1            | 5       | BARN       | 24.05.2023  |
      | 2            | 1       | SØKER      | 15.10.1988  |
      | 2            | 2       | BARN       | 11.09.2016  |
      | 2            | 3       | BARN       | 06.12.2017  |
      | 2            | 4       | BARN       | 17.06.2020  |
      | 2            | 5       | BARN       | 24.05.2023  |

    Og dagens dato er 24.06.2024
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 2            | 2       |
      | 2            | 3       |
      | 2            | 4       |

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 24.05.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 5       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 24.05.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 5       | GIFT_PARTNERSKAP                            |                  | 24.05.2023 |            | OPPFYLT  | Nei                  |                      |                  |
      | 5       | UNDER_18_ÅR                                 |                  | 24.05.2023 | 23.05.2041 | OPPFYLT  | Nei                  |                      |                  |


    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                      | Utdypende vilkår   | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser         | Vurderes etter   |
      | 1       | BOSATT_I_RIKET                              | VURDERT_MEDLEMSKAP | 30.09.2022 |            | IKKE_OPPFYLT | Ja                   | AVSLAG_MEDLEM_I_FOLKETRYGDEN | NASJONALE_REGLER |
      | 1       | LOVLIG_OPPHOLD                              |                    | 04.10.2022 |            | OPPFYLT      | Nei                  |                              | NASJONALE_REGLER |

      | 2       | UNDER_18_ÅR                                 |                    | 11.09.2016 | 10.09.2034 | OPPFYLT      | Nei                  |                              |                  |
      | 2       | GIFT_PARTNERSKAP                            |                    | 11.09.2016 |            | OPPFYLT      | Nei                  |                              |                  |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER                |                    | 30.09.2022 |            | OPPFYLT      | Nei                  |                              | NASJONALE_REGLER |
      | 2       | LOVLIG_OPPHOLD                              |                    | 04.10.2022 |            | OPPFYLT      | Nei                  |                              | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR                                 |                    | 06.12.2017 | 05.12.2035 | OPPFYLT      | Nei                  |                              |                  |
      | 3       | GIFT_PARTNERSKAP                            |                    | 06.12.2017 |            | OPPFYLT      | Nei                  |                              |                  |
      | 3       | BOR_MED_SØKER,BOSATT_I_RIKET                |                    | 30.09.2022 |            | OPPFYLT      | Nei                  |                              | NASJONALE_REGLER |
      | 3       | LOVLIG_OPPHOLD                              |                    | 04.10.2022 |            | OPPFYLT      | Nei                  |                              | NASJONALE_REGLER |

      | 4       | UNDER_18_ÅR                                 |                    | 17.06.2020 | 16.06.2038 | OPPFYLT      | Nei                  |                              |                  |
      | 4       | GIFT_PARTNERSKAP                            |                    | 17.06.2020 |            | OPPFYLT      | Nei                  |                              |                  |
      | 4       | BOSATT_I_RIKET,BOR_MED_SØKER                |                    | 30.09.2022 |            | OPPFYLT      | Nei                  |                              | NASJONALE_REGLER |
      | 4       | LOVLIG_OPPHOLD                              |                    | 04.10.2022 |            | OPPFYLT      | Nei                  |                              | NASJONALE_REGLER |

      | 5       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                    | 24.05.2023 |            | OPPFYLT      | Nei                  |                              | NASJONALE_REGLER |
      | 5       | UNDER_18_ÅR                                 |                    | 24.05.2023 | 23.05.2041 | OPPFYLT      | Nei                  |                              |                  |
      | 5       | GIFT_PARTNERSKAP                            |                    | 24.05.2023 |            | OPPFYLT      | Nei                  |                              |                  |


    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 5       | 1            | 01.06.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 5       | 1            | 01.07.2023 | 30.04.2029 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 5       | 1            | 01.05.2029 | 30.04.2041 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |

    Når vedtaksperioder med begrunnelser genereres for behandling 2

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar | Begrunnelser                 |
      | 01.10.2022 |          | AVSLAG             |           | AVSLAG_MEDLEM_I_FOLKETRYGDEN |
      | 01.06.2023 |          | OPPHØR             |           |                              |
