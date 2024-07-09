# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder med valutajustering

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | Nei                       | EØS                 | AVSLUTTET         |
      | 2            | 1        | 1                   | FORTSATT_OPPHØRT    | ÅRLIG_KONTROLL   | Nei                       | EØS                 | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 14.03.1989  |              |
      | 1            | 2       | BARN       | 02.07.2020  |              |
      | 2            | 1       | SØKER      | 14.03.1989  |              |
      | 2            | 2       | BARN       | 02.07.2020  |              |

  Scenario: Skal ikke generere vedtaksperioder med bare valutajustering
    Og dagens dato er 20.06.2024
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                              | 14.03.1989 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 15.03.2024 | 15.06.2024 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | LOVLIG_OPPHOLD   |                              | 02.07.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | GIFT_PARTNERSKAP |                              | 02.07.2020 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR      |                              | 02.07.2020 | 01.07.2038 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 02.07.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_NORGE_MED_SØKER   | 02.07.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                              | 14.03.1989 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 15.03.2024 | 15.06.2024 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | LOVLIG_OPPHOLD   |                              | 02.07.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | UNDER_18_ÅR      |                              | 02.07.2020 | 01.07.2038 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_NORGE_MED_SØKER   | 02.07.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 02.07.2020 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | GIFT_PARTNERSKAP |                              | 02.07.2020 |            | OPPFYLT  | Nei                  |                      |                  |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.04.2024 | 30.06.2024 | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | DK                    | NO                             | DK                  |
      | 2       | 01.04.2024 | 30.06.2024 | NORGE_ER_SEKUNDÆRLAND | 2            | ARBEIDER         | I_ARBEID                  | DK                    | NO                             | DK                  |

    Og med utenlandsk periodebeløp
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2       | 04.2024   | 06.2024   | 1            | 700   | DKK         | MÅNEDLIG  | DK              |
      | 2       | 04.2024   | 04.2024   | 2            | 700   | DKK         | MÅNEDLIG  | DK              |
      | 2       | 05.2024   | 06.2024   | 2            | 800   | DKK         | MÅNEDLIG  | DK              |

    Og med valutakurser
      | AktørId | Fra dato   | Til dato   | BehandlingId | Valutakursdato | Valuta kode | Kurs         | Vurderingsform |
      | 2       | 01.06.2024 | 30.06.2024 | 1            | 2024-05-31     | DKK         | 1.5261168016 | AUTOMATISK     |
      | 2       | 01.04.2024 | 30.04.2024 | 1            | 2024-03-27     | DKK         | 1.5663967177 | AUTOMATISK     |
      | 2       | 01.05.2024 | 31.05.2024 | 1            | 2024-04-30     | DKK         | 1.5841411582 | AUTOMATISK     |
      | 2       | 01.04.2024 | 30.04.2024 | 2            | 2024-03-27     | DKK         | 1.5663967177 | AUTOMATISK     |
      | 2       | 01.05.2024 | 31.05.2024 | 2            | 2024-04-30     | DKK         | 1.5841411582 | AUTOMATISK     |
      | 2       | 01.06.2024 | 30.06.2024 | 2            | 2024-05-31     | DKK         | 1.5261168016 | AUTOMATISK     |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.04.2024 | 30.04.2024 | 670   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.05.2024 | 31.05.2024 | 658   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.06.2024 | 30.06.2024 | 698   | ORDINÆR_BARNETRYGD | 100     | 1766 |

      | 2       | 2            | 01.04.2024 | 30.04.2024 | 670   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 2            | 01.05.2024 | 31.05.2024 | 499   | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 2            | 01.06.2024 | 30.06.2024 | 546   | ORDINÆR_BARNETRYGD | 100     | 1766 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent følgende vedtaksperioder for behandling 2
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar     |
      | 01.05.2024 | 30.06.2024 | Utbetaling         | Barn og søker |
      | 01.07.2024 |            | Opphør             | Barn og søker |
