# language: no
# encoding: UTF-8

Egenskap: Behandlingsresultat

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId  | Fagsaktype |
      | 200057251 | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId  | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak |
      | 100175302    | 200057251 |                     | INNVILGET           | SØKNAD           |
      | 100175351    | 200057251 | 100175302           | FORTSATT_INNVILGET  | ÅRLIG_KONTROLL   |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId       | Persontype | Fødselsdato |
      | 100175302    | 2184579750380 | BARN       | 07.10.2021  |
      | 100175302    | 2195613268073 | SØKER      | 27.04.1991  |
      | 100175351    | 2184579750380 | BARN       | 07.10.2021  |
      | 100175351    | 2195613268073 | SØKER      | 27.04.1991  |

  Scenario: Skal gi riktige perioder når behandlingsresultatet er fortsatt innvilget
    Og lag personresultater for begrunnelse for behandling 100175302
    Og lag personresultater for begrunnelse for behandling 100175351

    Og legg til nye vilkårresultater for begrunnelse for behandling 100175302
      | AktørId       | Vilkår                          | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2184579750380 | LOVLIG_OPPHOLD,GIFT_PARTNERSKAP |                              | 07.10.2021 |            | OPPFYLT  | Nei                  |
      | 2184579750380 | BOR_MED_SØKER                   | BARN_BOR_I_EØS_MED_SØKER     | 07.10.2021 |            | OPPFYLT  | Nei                  |
      | 2184579750380 | UNDER_18_ÅR                     |                              | 07.10.2021 | 06.10.2039 | OPPFYLT  | Nei                  |
      | 2184579750380 | BOSATT_I_RIKET                  | BARN_BOR_I_EØS               | 07.10.2021 |            | OPPFYLT  | Nei                  |

      | 2195613268073 | LOVLIG_OPPHOLD                  |                              | 27.04.1991 |            | OPPFYLT  | Nei                  |
      | 2195613268073 | BOSATT_I_RIKET                  | OMFATTET_AV_NORSK_LOVGIVNING | 11.05.2021 |            | OPPFYLT  | Nei                  |

    Og legg til nye vilkårresultater for begrunnelse for behandling 100175351
      | AktørId       | Vilkår                          | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2195613268073 | LOVLIG_OPPHOLD                  |                              | 27.04.1991 |            | OPPFYLT  | Nei                  |
      | 2195613268073 | BOSATT_I_RIKET                  | OMFATTET_AV_NORSK_LOVGIVNING | 11.05.2021 |            | OPPFYLT  | Nei                  |

      | 2184579750380 | BOR_MED_SØKER                   | BARN_BOR_I_EØS_MED_SØKER     | 07.10.2021 |            | OPPFYLT  | Nei                  |
      | 2184579750380 | LOVLIG_OPPHOLD,GIFT_PARTNERSKAP |                              | 07.10.2021 |            | OPPFYLT  | Nei                  |
      | 2184579750380 | BOSATT_I_RIKET                  | BARN_BOR_I_EØS               | 07.10.2021 |            | OPPFYLT  | Nei                  |
      | 2184579750380 | UNDER_18_ÅR                     |                              | 07.10.2021 | 06.10.2039 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent |
      | 2184579750380 | 100175302    | 01.11.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     |
      | 2184579750380 | 100175302    | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     |
      | 2184579750380 | 100175302    | 01.07.2023 | 30.09.2027 | 1766  | ORDINÆR_BARNETRYGD | 100     |
      | 2184579750380 | 100175302    | 01.10.2027 | 30.09.2039 | 1310  | ORDINÆR_BARNETRYGD | 100     |
      | 2184579750380 | 100175302    | 01.03.2022 | 30.11.2022 | 553   | ORDINÆR_BARNETRYGD | 100     |
      | 2184579750380 | 100175302    | 01.01.2022 | 28.02.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     |
      | 2184579750380 | 100175302    | 01.12.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     |
      | 2184579750380 | 100175351    | 01.11.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     |
      | 2184579750380 | 100175351    | 01.03.2023 | 30.06.2023 | 1723  | ORDINÆR_BARNETRYGD | 100     |
      | 2184579750380 | 100175351    | 01.07.2023 | 30.09.2027 | 1766  | ORDINÆR_BARNETRYGD | 100     |
      | 2184579750380 | 100175351    | 01.10.2027 | 30.09.2039 | 1310  | ORDINÆR_BARNETRYGD | 100     |
      | 2184579750380 | 100175351    | 01.01.2022 | 28.02.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     |
      | 2184579750380 | 100175351    | 01.03.2022 | 30.11.2022 | 553   | ORDINÆR_BARNETRYGD | 100     |
      | 2184579750380 | 100175351    | 01.12.2022 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     |

    Og med kompetanser for begrunnelse
      | AktørId       | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2184579750380 | 01.11.2021 | 28.02.2022 | NORGE_ER_PRIMÆRLAND   | 100175302    | ARBEIDER         | INAKTIV                   | NO                    | PL                             | PL                  |
      | 2184579750380 | 01.03.2022 | 30.11.2022 | NORGE_ER_SEKUNDÆRLAND | 100175302    | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |
      | 2184579750380 | 01.12.2022 |            | NORGE_ER_PRIMÆRLAND   | 100175302    | ARBEIDER         | INAKTIV                   | NO                    | PL                             | PL                  |
      | 2184579750380 | 01.11.2021 | 28.02.2022 | NORGE_ER_PRIMÆRLAND   | 100175351    | ARBEIDER         | INAKTIV                   | NO                    | PL                             | PL                  |
      | 2184579750380 | 01.03.2022 | 30.11.2022 | NORGE_ER_SEKUNDÆRLAND | 100175351    | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |
      | 2184579750380 | 01.12.2022 |            | NORGE_ER_PRIMÆRLAND   | 100175351    | ARBEIDER         | INAKTIV                   | NO                    | PL                             | PL                  |

    Når begrunnelsetekster genereres for behandling 100175351

    Og følgende dagens dato 11.09.2023

    Så forvent følgende standardBegrunnelser
      | Fra dato | Til dato | VedtaksperiodeType | Regelverk | Inkluderte Begrunnelser | Ekskluderte Begrunnelser |
      |          |          | FORTSATT_INNVILGET |           |                         |                          |