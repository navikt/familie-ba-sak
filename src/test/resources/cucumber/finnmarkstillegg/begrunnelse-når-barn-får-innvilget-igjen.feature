# language: no
# encoding: UTF-8

Egenskap: Plassholdertekst for egenskap - WeFud4GdEH

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | NYE_OPPLYSNINGER | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 25.11.1979  |              |
      | 1            | 2       | BARN       | 09.09.2009  |              |
      | 1            | 3       | BARN       | 23.06.2011  |              |
      | 2            | 1       | SØKER      | 25.11.1979  |              |
      | 2            | 2       | BARN       | 09.09.2009  |              |
      | 2            | 3       | BARN       | 23.06.2011  |              |

  Scenario: Plassholdertekst for scenario - Acl4XeUw06
    Og dagens dato er 16.09.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår             | Fra dato   | Til dato   | Resultat |
      | 1       | LOVLIG_OPPHOLD               |                              | 25.11.1979 |            | OPPFYLT  |
      | 1       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 09.09.2009 | 01.07.2025 | OPPFYLT  |
      | 1       | BOSATT_I_RIKET               |                              | 02.07.2025 |            | OPPFYLT  |

      | 2       | LOVLIG_OPPHOLD,BOR_MED_SØKER |                              | 09.09.2009 |            | OPPFYLT  |
      | 2       | GIFT_PARTNERSKAP             |                              | 09.09.2009 |            | OPPFYLT  |
      | 2       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 09.09.2009 |            | OPPFYLT  |
      | 2       | UNDER_18_ÅR                  |                              | 09.09.2009 | 08.09.2027 | OPPFYLT  |

      | 3       | UNDER_18_ÅR                  |                              | 23.06.2011 | 22.06.2029 | OPPFYLT  |
      | 3       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 23.06.2011 |            | OPPFYLT  |
      | 3       | BOR_MED_SØKER,LOVLIG_OPPHOLD |                              | 23.06.2011 |            | OPPFYLT  |
      | 3       | GIFT_PARTNERSKAP             |                              | 23.06.2011 |            | OPPFYLT  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår             | Fra dato   | Til dato   | Resultat |
      | 1       | LOVLIG_OPPHOLD               |                              | 25.11.1979 |            | OPPFYLT  |
      | 1       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 09.09.2009 |            | OPPFYLT  |

      | 2       | BOR_MED_SØKER,LOVLIG_OPPHOLD |                              | 09.09.2009 |            | OPPFYLT  |
      | 2       | GIFT_PARTNERSKAP             |                              | 09.09.2009 |            | OPPFYLT  |
      | 2       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 09.09.2009 |            | OPPFYLT  |
      | 2       | UNDER_18_ÅR                  |                              | 09.09.2009 | 08.09.2027 | OPPFYLT  |

      | 3       | BOR_MED_SØKER,LOVLIG_OPPHOLD |                              | 23.06.2011 |            | OPPFYLT  |
      | 3       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 23.06.2011 |            | OPPFYLT  |
      | 3       | GIFT_PARTNERSKAP             |                              | 23.06.2011 |            | OPPFYLT  |
      | 3       | UNDER_18_ÅR                  |                              | 23.06.2011 | 22.06.2029 | OPPFYLT  |

    Og med endrede utbetalinger
      | AktørId | BehandlingId | Fra dato   | Til dato   | Årsak              | Prosent |
      | 2       | 1            | 01.10.2009 | 30.06.2011 | ETTERBETALING_3MND | 0       |
      | 3,2     | 1            | 01.07.2011 | 31.05.2025 | ETTERBETALING_3MND | 0       |
      | 2       | 2            | 01.10.2009 | 30.06.2011 | ETTERBETALING_3MND | 0       |
      | 3,2     | 2            | 01.07.2011 | 31.05.2025 | ETTERBETALING_3MND | 0       |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.10.2009 | 28.02.2019 | 0     | ORDINÆR_BARNETRYGD | 0       | 970  |
      | 2       | 1            | 01.03.2019 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1083 |
      | 2       | 1            | 01.07.2023 | 31.12.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1310 |
      | 2       | 1            | 01.01.2024 | 31.08.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1510 |
      | 2       | 1            | 01.09.2024 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 1            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 1            | 01.06.2025 | 31.08.2027 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 1            | 01.07.2011 | 28.02.2019 | 0     | ORDINÆR_BARNETRYGD | 0       | 970  |
      | 3       | 1            | 01.03.2019 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1083 |
      | 3       | 1            | 01.07.2023 | 31.12.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1310 |
      | 3       | 1            | 01.01.2024 | 31.08.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1510 |
      | 3       | 1            | 01.09.2024 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 3       | 1            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 3       | 1            | 01.06.2025 | 31.05.2029 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |

      | 2       | 2            | 01.10.2009 | 28.02.2019 | 0     | ORDINÆR_BARNETRYGD | 0       | 970  |
      | 2       | 2            | 01.03.2019 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1083 |
      | 2       | 2            | 01.07.2023 | 31.12.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1310 |
      | 2       | 2            | 01.01.2024 | 31.08.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1510 |
      | 2       | 2            | 01.09.2024 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 2       | 2            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 2       | 2            | 01.06.2025 | 31.08.2027 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 2       | 2            | 01.08.2025 | 31.08.2027 | 500   | FINNMARKSTILLEGG   | 100     | 500  |
      | 3       | 2            | 01.07.2011 | 28.02.2019 | 0     | ORDINÆR_BARNETRYGD | 0       | 970  |
      | 3       | 2            | 01.03.2019 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1054 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1083 |
      | 3       | 2            | 01.07.2023 | 31.12.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1310 |
      | 3       | 2            | 01.01.2024 | 31.08.2024 | 0     | ORDINÆR_BARNETRYGD | 0       | 1510 |
      | 3       | 2            | 01.09.2024 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 3       | 2            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 3       | 2            | 01.06.2025 | 31.05.2029 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 2            | 01.08.2025 | 31.05.2029 | 500   | FINNMARKSTILLEGG   | 100     | 500  |

    Når vedtaksperiodene genereres for behandling 2


    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser       | Ugyldige begrunnelser |
      | 01.08.2025 | 31.08.2027 | UTBETALING         |                                | INNVILGET_FINNMARKSTILLEGG |                       |
      | 01.09.2027 | 31.05.2029 | UTBETALING         |                                |                            |                       |
      | 01.06.2029 |            | OPPHØR             |                                |                            |                       |
