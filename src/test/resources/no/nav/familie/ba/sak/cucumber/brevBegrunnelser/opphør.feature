# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser ved opphør

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId  | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId  | ForrigeBehandlingId | Behandlingsresultat  | Behandlingsårsak | Skal behandles automatisk |
      | 1            | 1        |                     | INNVILGET_OG_OPPHØRT | SØKNAD           | Nei                       |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId       | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 10.07.1988  |
      | 1            | 2       | BARN       | 17.04.2017  |
      | 1            | 3       | BARN       | 11.07.2013  |
      | 1            | 4       | BARN       | 16.04.2017  |

  Scenario: Skal kun flette inn barna som hadde andeler forrige periode når vi opphører på søker.
    Og følgende dagens dato 13.10.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId       | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD                                               |                  | 10.07.1988 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                                               |                  | 01.12.2022 | 01.02.2023 | OPPFYLT  | Nei                  |

      | 3       | GIFT_PARTNERSKAP,BOR_MED_SØKER,LOVLIG_OPPHOLD                |                  | 11.07.2013 |            | OPPFYLT  | Nei                  |
      | 3       | UNDER_18_ÅR                                                  |                  | 11.07.2013 | 10.07.2031 | OPPFYLT  | Nei                  |
      | 3       | BOSATT_I_RIKET                                               |                  | 01.12.2022 | 01.01.2023 | OPPFYLT  | Nei                  |

      | 2       | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 17.04.2017 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                                  |                  | 17.04.2017 | 16.04.2035 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3       | 1            | 01.01.2023 | 31.01.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.01.2023 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |

    Når vedtaksperiodene genereres for behandling 1

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato | Standardbegrunnelser | Eøsbegrunnelser | Fritekster |
      | 01.03.2023 |          | OPPHØR_UTVANDRET     |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.03.2023 til -
      | Begrunnelse      | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp |
      | OPPHØR_UTVANDRET | STANDARD | Ja            | 17.04.17             | 1           | februar 2023                         | NB      | 0     |

  Scenario: Skal flette inn barn som har oppfylte vilkår og barn som hadde andeler i forrige periode når vi opphører for søker
    Og følgende dagens dato 13.10.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD                                               |                  | 10.07.1988 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                                               |                  | 01.12.2022 | 01.02.2023 | OPPFYLT  | Nei                  |

      | 2       | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 17.04.2017 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                                  |                  | 17.04.2017 | 16.04.2035 | OPPFYLT  | Nei                  |

      | 4       | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOR_MED_SØKER                |                  | 17.04.2017 |            | OPPFYLT  | Nei                  |
      | 4       | BOSATT_I_RIKET                                               |                  | 17.04.2017 | 01.02.2023 | OPPFYLT  | Nei                  |
      | 4       | UNDER_18_ÅR                                                  |                  | 17.04.2017 | 16.04.2035 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.01.2023 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 4       | 1            | 01.01.2023 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |

    Når vedtaksperiodene genereres for behandling 1

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato | Standardbegrunnelser | Eøsbegrunnelser | Fritekster |
      | 01.03.2023 |          | OPPHØR_UTVANDRET     |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.03.2023 til -
      | Begrunnelse      | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp |
      | OPPHØR_UTVANDRET | STANDARD | Ja            | 16.04.17 og 17.04.17 | 2           | februar 2023                         | NB      | 0     |