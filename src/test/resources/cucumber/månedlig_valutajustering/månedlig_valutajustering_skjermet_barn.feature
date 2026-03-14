# language: no
# encoding: UTF-8

Egenskap: Plassholdertekst for egenskap - s38eEPJdCs

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype    | Status  |
      | 1        | SKJERMET_BARN | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | EØS                 | AVSLUTTET         |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 22.02.1986  |
      | 1            | 2       | BARN       | 01.11.2025  |

  Scenario: Plassholdertekst for scenario - rVokeDEt0A
    Og dagens dato er 01.03.2026
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Vurderes etter   |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 22.02.1986 |            | OPPFYLT  | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD   |                              | 22.02.1986 |            | OPPFYLT  | EØS_FORORDNINGEN |

      | 2       | BOR_MED_SØKER    | BARN_BOR_I_NORGE_MED_SØKER   | 01.11.2025 |            | OPPFYLT  | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 01.11.2025 |            | OPPFYLT  | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 01.11.2025 |            | OPPFYLT  | EØS_FORORDNINGEN |
      | 2       | GIFT_PARTNERSKAP |                              | 01.11.2025 |            | OPPFYLT  |                  |
      | 2       | UNDER_18_ÅR      |                              | 01.11.2025 | 31.10.2043 | OPPFYLT  |                  |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.12.2025 |          | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | SE                             | SE                  |

    Og med utenlandsk periodebeløp
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2       | 12.2025   |           | 1            | 500   | SEK         | MÅNEDLIG  | SE              |

    Og med valutakurser
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2       | 01.12.2025 | 31.12.2025 | 1            | 2025-11-28     | SEK         | 1.0724736770 | AUTOMATISK     |
      | 2       | 01.01.2026 | 31.01.2026 | 1            | 2025-12-31     | SEK         | 1.0943954165 | AUTOMATISK     |
      | 2       | 01.02.2026 |            | 1            | 2026-01-30     | SEK         | 1.0825055843 | AUTOMATISK     |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.12.2025 | 31.12.2025 | 1432  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 2       | 1            | 01.01.2026 | 31.01.2026 | 1421  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 2       | 1            | 01.02.2026 | 31.10.2043 | 1471  | ORDINÆR_BARNETRYGD | 100     | 2012 |

    Når vi lager automatisk behandling med id 2 på fagsak 1 på grunn av automatisk valutajustering og har følgende valutakurser
      | Valuta kode | Valutakursdato | Kurs         |
      | SEK         | 2025-11-28     | 1.0724736770 |
      | SEK         | 2025-12-31     | 1.0943954165 |
      | SEK         | 2026-01-30     | 1.0825055843 |
      | SEK         | 2026-02-27     | 1.0510300723 |

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats | Differanseberegnet beløp |
      | 2       | 2            | 01.12.2025 | 31.12.2025 | 1432  | ORDINÆR_BARNETRYGD | 100     | 1968 | 1432                     |
      | 2       | 2            | 01.01.2026 | 31.01.2026 | 1421  | ORDINÆR_BARNETRYGD | 100     | 1968 | 1421                     |
      | 2       | 2            | 01.02.2026 | 28.02.2026 | 1471  | ORDINÆR_BARNETRYGD | 100     | 2012 | 1471                     |
      | 2       | 2            | 01.03.2026 | 31.10.2043 | 1487  | ORDINÆR_BARNETRYGD | 100     | 2012 | 1487                     |