# language: no
# encoding: UTF-8

Egenskap: Automatisk valutajustering

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus | Endret tidspunkt |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | Nei                       | EØS                 | Avsluttet         | 17.02.2020       |
      | 2            | 1        | 1                   | INNVILGET           | SØKNAD           | Nei                       | EØS                 | Utredes           | 17.02.2023       |


    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 17.02.1990  |              |
      | 1            | 2       | BARN       | 29.10.2019  |              |
      | 2            | 1       | SØKER      | 17.02.1990  |              |
      | 2            | 2       | BARN       | 29.10.2019  |              |


  Scenario: Skal kunne kjøre automatisk valutajustering og snike i køen
    Og dagens dato er 13.03.2024
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                              | 17.02.1990 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 01.06.2023 | 30.09.2025 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | UNDER_18_ÅR      |                              | 29.10.2019 | 28.10.2037 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 29.10.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_NORGE_MED_SØKER   | 29.10.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 29.10.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | GIFT_PARTNERSKAP |                              | 29.10.2019 |            | OPPFYLT  | Nei                  |                      |                  |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.11.2019 |          | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | DK                             | DK                  |

    Og med utenlandsk periodebeløp
      | AktørId | Fra måned | Til måned | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2       | 11.2019   | 02.2024   | 1            | 100   | DKK         | MÅNEDLIG  | DK              |
      | 2       | 03.2024   |           | 1            | 200   | DKK         | MÅNEDLIG  | DK              |

    Og med valutakurser
      | AktørId | Fra dato   | Til dato | BehandlingId | Valutakursdato | Valuta kode | Kurs |
      | 2       | 01.11.2019 |          | 1            | 01.11.2019     | DKK         | 2    |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.07.2023 | 30.09.2025 | 1566  | ORDINÆR_BARNETRYGD | 100     |  1766|
      | 2       | 2            | 01.07.2023 | 30.09.2025 | 1566  | ORDINÆR_BARNETRYGD | 100     | 1766 |



    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                              | 17.02.1990 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 01.06.2023 | 30.09.2025 | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | UNDER_18_ÅR      |                              | 29.10.2019 | 28.10.2037 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 29.10.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_NORGE_MED_SØKER   | 29.10.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 29.10.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | GIFT_PARTNERSKAP |                              | 29.10.2019 |            | OPPFYLT  | Nei                  |                      |                  |

    Når vi lager automatisk behandling med id 3 på fagsak 1 på grunn av automatisk valutajustering og har følgende valutakurser
      | Valuta kode | Valutakursdato | Kurs |
      | DKK         | 29.02.2024     | 3    |

    Så forvent disse behandlingene
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak         | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus | Behandlingstype       | Behandlingssteg           | Underkategori |
      | 3            | 1        | 1                   | ENDRET_UTBETALING   | MÅNEDLIG_VALUTAJUSTERING | Ja                        | EØS                 | AVSLUTTET         | Revurdering           | BEHANDLING_AVSLUTTET      | ORDINÆR       |
      | 2            | 1        | 1                   | INNVILGET           | SØKNAD                   | Nei                       | EØS                 | UTREDES           | FØRSTEGANGSBEHANDLING | REGISTRERE_PERSONGRUNNLAG | ORDINÆR       |
