# language: no
# encoding: UTF-8

Egenskap: Autovedtak satsendring

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype    |
      | 1        | SKJERMET_BARN |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | Behandlingsresultat | Behandlingsårsak | Behandlingsstatus |
      | 1            | 1        | INNVILGET           | SØKNAD           | AVSLUTTET         |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 01.01.2000  |
      | 1            | 2       | BARN       | 15.11.2025  |

  Scenario: Skal oppdatere vilkårresultater og generere andeler når autovedtak satsendring kjøres
    Og dagens dato er 01.03.2026

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                        | Fra dato   | Til dato   | Resultat |
      | 1       | BOSATT_I_RIKET, LOVLIG_OPPHOLD                | 01.01.2025 |            | OPPFYLT  |

      | 2       | UNDER_18_ÅR                                   | 15.11.2025 | 14.11.2043 | OPPFYLT  |
      | 2       | GIFT_PARTNERSKAP                              | 15.11.2025 |            | OPPFYLT  |
      | 2       | BOSATT_I_RIKET, BOR_MED_SØKER, LOVLIG_OPPHOLD | 15.11.2025 |            | OPPFYLT  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Prosent | Ytelse type        |
      | 2       | 1            | 01.12.2025 | 31.10.2043 | 1968  | 100     | ORDINÆR_BARNETRYGD |

    Når vi lager automatisk behandling med id 2 på fagsak 1 på grunn av satsendring

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Prosent | Ytelse type        |
      | 2       | 2            | 01.12.2025 | 31.01.2026 | 1968  | 100     | ORDINÆR_BARNETRYGD |
      | 2       | 2            | 01.02.2026 | 31.10.2043 | 2012  | 100     | ORDINÆR_BARNETRYGD |