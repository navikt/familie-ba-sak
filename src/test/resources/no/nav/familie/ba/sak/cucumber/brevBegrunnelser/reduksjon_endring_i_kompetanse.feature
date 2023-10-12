# language: no
# encoding: UTF-8

Egenskap: Plassholdertekst for egenskap - QS5V7LQWC5

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId       | Persontype | Fødselsdato |
      | 1            | 2612578516528 | SØKER      | 16.07.1985  |
      | 1            | 2251646180408 | BARN       | 18.06.2019  |
      | 1            | 2331997245661 | BARN       | 20.12.2014  |

  Scenario: Plassholdertekst for scenario - EMG8rSEEJw
    Og følgende dagens dato 12.10.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId       | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2251646180408 | UNDER_18_ÅR      |                              | 18.06.2019 | 17.06.2037 | OPPFYLT  | Nei                  |
      | 2251646180408 | GIFT_PARTNERSKAP |                              | 18.06.2019 |            | OPPFYLT  | Nei                  |
      | 2251646180408 | LOVLIG_OPPHOLD   |                              | 18.05.2022 |            | OPPFYLT  | Nei                  |
      | 2251646180408 | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 18.05.2022 |            | OPPFYLT  | Nei                  |
      | 2251646180408 | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 18.05.2022 |            | OPPFYLT  | Nei                  |

      | 2331997245661 | UNDER_18_ÅR      |                              | 20.12.2014 | 19.12.2032 | OPPFYLT  | Nei                  |
      | 2331997245661 | GIFT_PARTNERSKAP |                              | 20.12.2014 |            | OPPFYLT  | Nei                  |
      | 2331997245661 | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 18.05.2022 |            | OPPFYLT  | Nei                  |
      | 2331997245661 | LOVLIG_OPPHOLD   |                              | 18.05.2022 |            | OPPFYLT  | Nei                  |
      | 2331997245661 | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 18.05.2022 |            | OPPFYLT  | Nei                  |

      | 2612578516528 | LOVLIG_OPPHOLD   |                              | 18.05.2022 |            | OPPFYLT  | Nei                  |
      | 2612578516528 | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 18.05.2022 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2251646180408 | 1            | 01.06.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2251646180408 | 1            | 01.03.2023 | 31.08.2023 | 0     | ORDINÆR_BARNETRYGD | 0       | 1723 |
      | 2251646180408 | 1            | 01.09.2023 | 31.05.2025 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2251646180408 | 1            | 01.06.2025 | 31.05.2037 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 2331997245661 | 1            | 01.06.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2331997245661 | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2331997245661 | 1            | 01.07.2023 | 30.11.2032 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med endrede utbetalinger for begrunnelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Årsak             | Prosent |
      | 2251646180408 | 1            | 01.03.2023 | 31.08.2023 | ALLEREDE_UTBETALT | 0       |

    Og med kompetanser for begrunnelse
      | AktørId                      | Fra dato   | Til dato   | Resultat            | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2331997245661, 2251646180408 | 01.09.2023 |            | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | BE                             | BE                  |
      | 2331997245661                | 01.03.2023 | 31.08.2023 | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | BE                             | BE                  |
      | 2331997245661, 2251646180408 | 01.06.2022 | 28.02.2023 | NORGE_ER_PRIMÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | BE                             | BE                  |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser | Eøsbegrunnelser        | Fritekster |
      | 01.03.2023 | 30.06.2023 |                      | REDUKSJON_BARN_DØD_EØS |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.03.2023 til 30.06.2023
      | Begrunnelse            | Type | Barnas fødselsdatoer | Antall barn | Målform | Annen forelders aktivitetsland | Barnets bostedsland | Søkers aktivitetsland | Annen forelders aktivitet | Søkers aktivitet |
      | REDUKSJON_BARN_DØD_EØS | EØS  | 18.06.19             | 1           | NB      | Belgia                         | Belgia              | Norge                 | I_ARBEID                  | ARBEIDER         |