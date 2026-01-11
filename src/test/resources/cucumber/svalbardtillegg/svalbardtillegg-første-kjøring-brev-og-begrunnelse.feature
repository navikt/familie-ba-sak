# language: no
# encoding: UTF-8

Egenskap: Svalbardtillegg autovedtak første kjøring - brev og begrunnelse

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
      | 1            | 3       | BARN       | 01.04.2025  |

    Og med følgende feature toggles
      | BehandlingId | FeatureToggleId                                                    | Er togglet på |
      | 2            | familie-ba-sak.skal-bruke-adressehendelseloype-for-svalbardtillegg | Nei           |


  Scenario: Skal oppdatere vilkårresultater og generere andeler når autovedtak svalbardtillegg kjøres
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
      | 2       | 1            | 01.05.2025 | 31.01.2026 | 1968  | 100     | ORDINÆR_BARNETRYGD |
      | 2       | 1            | 01.02.2026 | 31.12.2042 | 2012  | 100     | ORDINÆR_BARNETRYGD |

    Og med adressekommuner
      | AktørId | Fra dato   | Til dato | Kommunenummer | Adressetype     |
      | 1       | 01.01.2000 |          | 0301          | Bostedsadresse  |
      | 2       | 01.01.2025 |          | 0301          | Bostedsadresse  |
      | 1, 2    | 01.05.2025 |          | 2100          | Oppholdsadresse |

    Når vi lager automatisk behandling med id 2 på fagsak 1 på grunn av svalbardtillegg

    Så forvent følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                        | Fra dato   | Til dato   | Resultat | Utdypende vilkår   |
      | 1       | BOSATT_I_RIKET                | 01.01.2025 | 31.08.2025 | OPPFYLT  |                    |
      | 1       | BOSATT_I_RIKET                | 01.09.2025 |            | OPPFYLT  | BOSATT_PÅ_SVALBARD |
      | 1       | LOVLIG_OPPHOLD                | 01.01.2025 |            | OPPFYLT  |                    |

      | 2       | UNDER_18_ÅR                   | 01.01.2025 | 31.12.2042 | OPPFYLT  |                    |
      | 2       | GIFT_PARTNERSKAP              | 01.01.2025 |            | OPPFYLT  |                    |
      | 2       | BOSATT_I_RIKET                | 01.01.2025 | 31.08.2025 | OPPFYLT  |                    |
      | 2       | BOSATT_I_RIKET                | 01.09.2025 |            | OPPFYLT  | BOSATT_PÅ_SVALBARD |
      | 2       | BOR_MED_SØKER, LOVLIG_OPPHOLD | 01.01.2025 |            | OPPFYLT  |                    |

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Prosent | Ytelse type        |
      | 2       | 2            | 01.02.2025 | 30.04.2025 | 1766  | 100     | ORDINÆR_BARNETRYGD |
      | 2       | 2            | 01.05.2025 | 31.01.2026 | 1968  | 100     | ORDINÆR_BARNETRYGD |
      | 2       | 2            | 01.02.2026 | 31.12.2042 | 2012  | 100     | ORDINÆR_BARNETRYGD |
      | 2       | 2            | 01.10.2025 | 31.01.2026 | 500   | 100     | SVALBARDTILLEGG    |
      | 2       | 2            | 01.02.2026 | 31.12.2042 | 512   | 100     | SVALBARDTILLEGG    |

    Så forvent følgende vedtaksperioder for behandling 2
      | Fra dato   | Til dato   | Vedtaksperiodetype | Begrunnelser                        |
      | 01.09.2025 | 30.09.2025 | Utbetaling         |                                     |
      | 01.10.2025 | 31.01.2026 | Utbetaling         | INNVILGET_SVALBARDTILLEGG_UTEN_DATO |
      | 01.02.2026 | 31.12.2042 | Utbetaling         | INNVILGET_SVALBARDTILLEGG_UTEN_DATO |
      | 01.01.2043 |            | Opphør             |                                     |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.10.2025 til 31.01.2026
      | Begrunnelse                         | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | INNVILGET_SVALBARDTILLEGG_UTEN_DATO | STANDARD | Ja            | 01.01.25             | 1           | september 2025                       |         | 2 468 |                  | SØKER_HAR_IKKE_RETT     |                             |

    Så forvent at brevmal AUTOVEDTAK_SVALBARDTILLEGG er brukt for behandling 2