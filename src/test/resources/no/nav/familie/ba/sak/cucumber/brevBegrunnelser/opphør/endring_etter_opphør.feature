# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser ved endring etter opphør

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype | Fagsakstatus |
      | 1        | NORMAL     | LØPENDE      |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | NYE_OPPLYSNINGER | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 25.02.1975  |              |
      | 1            | 2       | BARN       | 07.01.2005  |              |
      | 1            | 3       | BARN       | 07.05.2006  |              |
      | 1            | 4       | BARN       | 11.05.2006  |              |
      | 2            | 1       | SØKER      | 25.02.1975  |              |
      | 2            | 2       | BARN       | 07.01.2005  |              |
      | 2            | 3       | BARN       | 07.05.2006  |              |
      | 2            | 4       | BARN       | 11.05.2006  |              |

  Scenario: Skal kun flette inn ett barn i begrunnelse når to barn fyller 18 samtidig, men det allerede har vært et opphør for det ene barnet
    Og følgende dagens dato 23.05.2024
    Og lag personresultater for begrunnelse for behandling 1
    Og lag personresultater for begrunnelse for behandling 2

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                  | 01.02.2022 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |

      | 2       | UNDER_18_ÅR                                 |                  | 07.01.2005 | 06.01.2023 | OPPFYLT      | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP                            |                  | 07.01.2005 |            | OPPFYLT      | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.02.2022 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR                                 |                  | 07.05.2006 | 06.05.2024 | OPPFYLT      | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP                            |                  | 07.05.2006 |            | OPPFYLT      | Nei                  |                      |                  |
      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 01.02.2022 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |

      | 4       | GIFT_PARTNERSKAP                            |                  | 11.05.2006 |            | OPPFYLT      | Nei                  |                      |                  |
      | 4       | UNDER_18_ÅR                                 |                  | 11.05.2006 | 10.05.2024 | OPPFYLT      | Nei                  |                      |                  |
      | 4       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 01.02.2022 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BOR_MED_SØKER                               |                  | 10.11.2023 | 08.01.2024 | IKKE_OPPFYLT | Ja                   | AVSLAG_BOR_HOS_SØKER | NASJONALE_REGLER |
      | 4       | BOR_MED_SØKER                               |                  | 09.01.2024 |            | OPPFYLT      | Nei                  |                      | NASJONALE_REGLER |

    Og legg til nye vilkårresultater for begrunnelse for behandling 2
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | UNDER_18_ÅR                                 |                  | 07.01.2005 | 06.01.2023 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP                            |                  | 07.01.2005 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | LOVLIG_OPPHOLD,BOR_MED_SØKER,BOSATT_I_RIKET |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | GIFT_PARTNERSKAP                            |                  | 07.05.2006 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | UNDER_18_ÅR                                 |                  | 07.05.2006 | 06.05.2024 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 4       | GIFT_PARTNERSKAP                            |                  | 11.05.2006 |            | OPPFYLT  | Nei                  |                      |                  |
      | 4       | UNDER_18_ÅR                                 |                  | 11.05.2006 | 10.05.2024 | OPPFYLT  | Nei                  |                      |                  |
      | 4       | LOVLIG_OPPHOLD,BOSATT_I_RIKET               |                  | 01.02.2022 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 4       | BOR_MED_SØKER                               |                  | 09.01.2024 | 06.03.2024 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.03.2022 | 31.12.2022 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.03.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 1            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 1            | 01.01.2024 | 30.04.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 4       | 1            | 01.02.2024 | 30.04.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |

      | 2       | 2            | 01.03.2022 | 31.12.2022 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 2            | 01.03.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 2            | 01.07.2023 | 31.12.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 2            | 01.01.2024 | 30.04.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 4       | 2            | 01.02.2024 | 31.03.2024 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |

    Når vedtaksperiodene genereres for behandling 2

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser   | Eøsbegrunnelser | Fritekster |
      | 01.04.2024 | 30.04.2024 | REDUKSJON_FLYTTET_BARN |                 |            |
      | 01.05.2024 |            | OPPHØR_UNDER_18_ÅR     |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.05.2024 til -
      | Begrunnelse        | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp |
      | OPPHØR_UNDER_18_ÅR | STANDARD | Nei           | 07.05.06             | 1           | april 2024                           | nb      | 0     |