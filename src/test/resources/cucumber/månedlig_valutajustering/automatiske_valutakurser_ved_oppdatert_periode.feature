# language: no
# encoding: UTF-8

Egenskap: Automatisk valutakurser ved oppdatering av kompetanseskjemaer

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status    |
      | 1        | NORMAL     | AVSLUTTET |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | IKKE_VURDERT        | SØKNAD           | Nei                       | EØS                 | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 02.03.1989  |              |
      | 1            | 2       | BARN       | 10.08.2013  |              |
      | 1            | 3       | BARN       | 25.06.2020  |              |

  Scenario: Skal automatisk oppdatere valutakursene riktig når vi oppdaterer utenlandsk periodebeløp
    Og dagens dato er 13.06.2024
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                              | 01.12.2023 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 29.03.2021 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | UNDER_18_ÅR      |                              | 10.08.2013 | 09.08.2031 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | GIFT_PARTNERSKAP |                              | 10.08.2013 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 10.08.2013 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_NORGE_MED_SØKER   | 10.08.2013 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 10.08.2013 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 25.06.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | GIFT_PARTNERSKAP |                              | 25.06.2020 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER    | BARN_BOR_I_NORGE_MED_SØKER   | 25.06.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 3       | UNDER_18_ÅR      |                              | 25.06.2020 | 24.06.2038 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | LOVLIG_OPPHOLD   |                              | 25.06.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2, 3    | 01.01.2024 |          | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | RO                             | RO                  |

    Og med utenlandsk periodebeløp
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 3       | 01.2024   | 03.2024   | 1            | 719   | RON         | MÅNEDLIG  | RO              |
      | 2       | 01.2024   | 03.2024   | 1            | 292   | RON         | MÅNEDLIG  | RO              |
      | 2, 3    | 04.2024   |           | 1            | 293   | RON         | MÅNEDLIG  | RO              |

    Og med valutakurser
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2, 3    | 01.01.2024 | 31.01.2024 | 1            | 2023-12-29     | RON         | 2.2591245277 | AUTOMATISK     |
      | 2, 3    | 01.02.2024 | 29.02.2024 | 1            | 2024-01-31     | RON         | 2.2812412074 | AUTOMATISK     |
      | 2, 3    | 01.03.2024 | 31.03.2024 | 1            | 2024-02-29     | RON         | 2.3119945278 | AUTOMATISK     |
      | 2, 3    | 01.04.2024 | 30.04.2024 | 1            | 2024-03-27     | RON         | 2.3496108284 | AUTOMATISK     |
      | 2, 3    | 01.05.2024 | 31.05.2024 | 1            | 2024-04-30     | RON         | 2.3744448241 | AUTOMATISK     |
      | 2, 3    | 01.06.2024 |            | 1            | 2024-05-31     | RON         | 2.2872586252 | AUTOMATISK     |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.01.2024 | 31.01.2024 | 851   | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 1            | 01.02.2024 | 29.02.2024 | 844   | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 1            | 01.03.2024 | 31.03.2024 | 835   | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 1            | 01.04.2024 | 30.04.2024 | 822   | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 1            | 01.05.2024 | 31.05.2024 | 815   | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 2       | 1            | 01.06.2024 | 31.07.2031 | 840   | ORDINÆR_BARNETRYGD | 100     | 1510 |
      | 3       | 1            | 01.01.2024 | 31.01.2024 | 142   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.02.2024 | 29.02.2024 | 126   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.03.2024 | 31.03.2024 | 104   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.04.2024 | 30.04.2024 | 1078  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.05.2024 | 31.05.2024 | 1071  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.06.2024 | 31.05.2026 | 1096  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.06.2026 | 31.05.2038 | 840   | ORDINÆR_BARNETRYGD | 100     | 1510 |

    Når vi legger til utenlandsk periodebeløp for behandling 1
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2, 3    | 04.2024   |           | 1            | 292   | RON         | MÅNEDLIG  | RO              |

    Så forvent følgende valutakurser for behandling 1
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2, 3    | 01.01.2024 | 31.01.2024 | 1            | 2023-12-29     | RON         | 2.2591245277 | AUTOMATISK     |
      | 2, 3    | 01.02.2024 | 29.02.2024 | 1            | 2024-01-31     | RON         | 2.2812412074 | AUTOMATISK     |
      | 2, 3    | 01.03.2024 | 31.03.2024 | 1            | 2024-02-29     | RON         | 2.3119945278 | AUTOMATISK     |
      | 2, 3    | 01.04.2024 | 30.04.2024 | 1            | 2024-03-27     | RON         | 2.3496108284 | AUTOMATISK     |
      | 2, 3    | 01.05.2024 | 31.05.2024 | 1            | 2024-04-30     | RON         | 2.3744448241 | AUTOMATISK     |
      | 2, 3    | 01.06.2024 |            | 1            | 2024-05-31     | RON         | 2.2872586252 | AUTOMATISK     |