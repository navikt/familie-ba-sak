# language: no
# encoding: UTF-8

Egenskap: Opphør ved overgang til EØS

  Scenario: Ved overgang fra Nasjonal til EØS med ikke oppfylte vilkår, skal riktige eøs begrunnelser vises
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status    |
      | 1        | NORMAL     | AVSLUTTET |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | OPPHØRT             | NYE_OPPLYSNINGER | Nei                       | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | NYE_OPPLYSNINGER | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 21.11.1989  |              |
      | 1            | 2       | BARN       | 27.06.2020  |              |
      | 2            | 1       | SØKER      | 21.11.1989  |              |
      | 2            | 2       | BARN       | 27.06.2020  |              |
    Og dagens dato er 07.12.2025
    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET                              |                  | 01.03.2022 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |
      | 1       | LOVLIG_OPPHOLD                              |                  | 01.03.2022 | 08.05.2022 | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |
      | 1       | LOVLIG_OPPHOLD                              |                  | 09.05.2022 |            | IKKE_OPPFYLT | Nei                  |                      | NASJONALE_REGLER |

      | 2       | UNDER_18_ÅR                                 |                  | 27.06.2020 | 26.06.2038 | OPPFYLT      | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP                            |                  | 27.06.2020 |            | OPPFYLT      | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER,LOVLIG_OPPHOLD |                  | 01.03.2022 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                      | Utdypende vilkår         | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                          | 01.03.2022 | 08.05.2022 | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                          | 09.05.2022 |            | IKKE_OPPFYLT | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | UNDER_18_ÅR                                 |                          | 27.06.2020 | 26.06.2038 | OPPFYLT      | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP                            |                          | 27.06.2020 |            | OPPFYLT      | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER,LOVLIG_OPPHOLD |                          | 01.03.2022 | 08.05.2022 | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |
      | 2       | BOR_MED_SØKER                               | BARN_BOR_I_EØS_MED_SØKER | 09.05.2022 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET                              |                          | 09.05.2022 |            | IKKE_OPPFYLT | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD                              |                          | 09.05.2022 |            | OPPFYLT      | Nei                  |                      | EØS_FORORDNINGEN |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.04.2022 | 31.05.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |

      | 2       | 2            | 01.04.2022 | 31.05.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser               | Ugyldige begrunnelser |
      | 01.06.2022 |          | OPPHØR             | EØS_FORORDNINGEN               | OPPHØR_IKKE_STATSBORGER_I_EØS_LAND |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser | Eøsbegrunnelser                    | Fritekster |
      | 01.06.2022 |          |                      | OPPHØR_IKKE_STATSBORGER_I_EØS_LAND |            |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.06.2022 til -
      | Begrunnelse                        | Type | Gjelder søker | Barnas fødselsdatoer | Antall barn |
      | OPPHØR_IKKE_STATSBORGER_I_EØS_LAND | EØS  | Ja            | 27.06.20             | 1           |