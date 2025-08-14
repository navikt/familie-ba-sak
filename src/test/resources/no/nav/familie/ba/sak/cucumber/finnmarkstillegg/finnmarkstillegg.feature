# language: no
# encoding: UTF-8

Egenskap: Finnmarkstillegg autovedtak

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | Behandlingsresultat | Behandlingsårsak | Behandlingsstatus |
      | 1            | 1        | INNVILGET           | SØKNAD           | AVSLUTTET         |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 01.01.2000  |
      | 1            | 2       | BARN       | 01.01.2025  |

  Scenario: Skal oppdatere vilkårresultater og generere andeler når autovedtak finnmarkstillegg kjøres
    Og dagens dato er 01.09.2025

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                        | Fra dato   | Til dato   | Resultat |
      | 1       | BOSATT_I_RIKET, LOVLIG_OPPHOLD                | 01.01.2025 |            | OPPFYLT  |

      | 2       | UNDER_18_ÅR                                   | 01.01.2025 | 31.12.2042 | OPPFYLT  |
      | 2       | GIFT_PARTNERSKAP                              | 01.01.2025 |            | OPPFYLT  |
      | 2       | BOSATT_I_RIKET, BOR_MED_SØKER, LOVLIG_OPPHOLD | 01.01.2025 |            | OPPFYLT  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Prosent | Ytelse type        |
      | 2       | 1            | 01.02.2025 | 30.04.2025 | 1766  | 100     | ORDINÆR_BARNETRYGD |
      | 2       | 1            | 01.05.2025 | 31.12.2042 | 1968  | 100     | ORDINÆR_BARNETRYGD |

    Og med bostedskommuner
      | AktørId | Fra dato   | Til dato | Kommunenummer |
      | 1       | 01.01.2000 |          | 0301          |
      | 2       | 01.01.2025 |          | 0301          |
      | 1, 2    | 01.05.2025 |          | 5601          |

    Når vi lager automatisk behandling med id 2 på fagsak 1 på grunn av finnmarkstillegg

    Så forvent følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                        | Fra dato   | Til dato   | Resultat | Utdypende vilkår             |
      | 1       | BOSATT_I_RIKET                | 01.01.2025 | 30.04.2025 | OPPFYLT  |                              |
      | 1       | BOSATT_I_RIKET                | 01.05.2025 |            | OPPFYLT  | BOSATT_I_FINNMARK_NORD_TROMS |
      | 1       | LOVLIG_OPPHOLD                | 01.01.2025 |            | OPPFYLT  |                              |

      | 2       | UNDER_18_ÅR                   | 01.01.2025 | 31.12.2042 | OPPFYLT  |                              |
      | 2       | GIFT_PARTNERSKAP              | 01.01.2025 |            | OPPFYLT  |                              |
      | 2       | BOSATT_I_RIKET                | 01.01.2025 | 30.04.2025 | OPPFYLT  |                              |
      | 2       | BOSATT_I_RIKET                | 01.05.2025 |            | OPPFYLT  | BOSATT_I_FINNMARK_NORD_TROMS |
      | 2       | BOR_MED_SØKER, LOVLIG_OPPHOLD | 01.01.2025 |            | OPPFYLT  |                              |

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Prosent | Ytelse type        |
      | 2       | 2            | 01.02.2025 | 30.04.2025 | 1766  | 100     | ORDINÆR_BARNETRYGD |
      | 2       | 2            | 01.05.2025 | 31.12.2042 | 1968  | 100     | ORDINÆR_BARNETRYGD |
      | 2       | 2            | 01.08.2025 | 31.12.2042 | 500   | 100     | FINNMARKSTILLEGG   |