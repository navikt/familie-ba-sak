# language: no
# encoding: UTF-8

Egenskap: Automatisk valutajustering

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | Nei                       | EØS                 | Avsluttet         |
      | 2            | 1        |  1                  | INNVILGET           | SØKNAD           | Nei                       | EØS                 | Utredes        |


    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 17.02.1990  |              |
      | 1            | 2       | BARN       | 29.10.2019  |              |

  Scenario: Skalkunne kjøre automatisk valutajustering
    Og følgende dagens dato 13.03.2024
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | LOVLIG_OPPHOLD   |                              | 17.02.1990 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 17.02.1990 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |

      | 2       | UNDER_18_ÅR      |                              | 29.10.2019 | 28.10.2037 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_NORGE             | 29.10.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_NORGE_MED_SØKER   | 29.10.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 29.10.2019 |            | OPPFYLT  | Nei                  |                      | EØS_FORORDNINGEN |
      | 2       | GIFT_PARTNERSKAP |                              | 29.10.2019 |            | OPPFYLT  | Nei                  |                      |                  |

    Og med utenlandsk periodebeløp for begrunnelse
      | AktørId | Fra dato   | Til dato | BehandlingId | Beløp | Valuta kode | Intervall | Utbetalingsland |
      | 2       | 01.11.2019 |          | 1            | 100   | DKK         | MÅNEDLIG  | NO              |

    Og med valutakurs for begrunnelse
      | AktørId | Fra dato   | Til dato | BehandlingId | Valutakursdato | Valuta kode | Kurs |
      | 2       | 01.11.2019 |          | 1            | 01.11.2019     | DKK         | 2    |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.11.2019 | 31.08.2020 | 854   | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.09.2020 | 31.08.2021 | 1154  | ORDINÆR_BARNETRYGD | 100     | 1354 |
      | 2       | 1            | 01.09.2021 | 31.12.2021 | 1454  | ORDINÆR_BARNETRYGD | 100     | 1654 |
      | 2       | 1            | 01.01.2022 | 28.02.2023 | 1476  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1523  | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 2       | 1            | 01.07.2023 | 30.09.2025 | 1566  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2       | 1            | 01.10.2025 | 30.09.2037 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1510 |

    Og med kompetanser for begrunnelse
      | AktørId | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.11.2019 |          | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | DK                    | DK                             | DK                  |

    Når vi lager automatisk behandling med id 2 på fagsak 1 på grunn av automatisk valutajustering

    Så forvent disse behandlingene
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak         | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus  | Behandlingstype | Behandlingssteg         | Underkategori |
      | 2            | 1        | 1                   | FORTSATT_INNVILGET  | MÅNEDLIG_VALUTAJUSTERING | Ja                        | EØS                 | IVERKSETTER_VEDTAK | Revurdering     | FERDIGSTILLE_BEHANDLING | ORDINÆR       |

    # Andelene må endres når vi faktisk oppdaterer valutakursen
    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats | Differanseberegnet beløp |
      | 2       | 2            | 01.11.2019 | 31.08.2020 | 854   | ORDINÆR_BARNETRYGD | 100     | 1054 | 854                      |
      | 2       | 2            | 01.09.2020 | 31.08.2021 | 1154  | ORDINÆR_BARNETRYGD | 100     | 1354 | 1154                     |
      | 2       | 2            | 01.09.2021 | 31.12.2021 | 1454  | ORDINÆR_BARNETRYGD | 100     | 1654 | 1454                     |
      | 2       | 2            | 01.01.2022 | 28.02.2023 | 1476  | ORDINÆR_BARNETRYGD | 100     | 1676 | 1476                     |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 1523  | ORDINÆR_BARNETRYGD | 100     | 1723 | 1523                     |
      | 2       | 2            | 01.07.2023 | 30.09.2025 | 1566  | ORDINÆR_BARNETRYGD | 100     | 1766 | 1566                     |
      | 2       | 2            | 01.10.2025 | 30.09.2037 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1510 | 1310                     |


# Test på at vi ikke endrer tilbake i tid
# Test på at grunnsatsen ikke er endra (feks ved gammel sats)