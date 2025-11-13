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

    Og med følgende feature toggles
      | BehandlingId | FeatureToggleId                                                     | Er togglet på |
      | 2            | familie-ba-sak.skal-bruke-adressehendelseloype-for-finnmarkstillegg | Ja            |


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

    Og med adressekommuner
      | AktørId | Fra dato   | Til dato | Kommunenummer |
      | 1       | 01.01.2000 |          | 0301          |
      | 2       | 01.01.2025 |          | 0301          |
      | 1, 2    | 01.05.2025 |          | 5601          |

    Når vi lager automatisk behandling med id 2 på fagsak 1 på grunn av finnmarkstillegg

    Så forvent følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                        | Fra dato   | Til dato   | Resultat | Utdypende vilkår             |
      | 1       | BOSATT_I_RIKET                | 01.01.2025 | 31.08.2025 | OPPFYLT  |                              |
      | 1       | BOSATT_I_RIKET                | 01.09.2025 |            | OPPFYLT  | BOSATT_I_FINNMARK_NORD_TROMS |
      | 1       | LOVLIG_OPPHOLD                | 01.01.2025 |            | OPPFYLT  |                              |

      | 2       | UNDER_18_ÅR                   | 01.01.2025 | 31.12.2042 | OPPFYLT  |                              |
      | 2       | GIFT_PARTNERSKAP              | 01.01.2025 |            | OPPFYLT  |                              |
      | 2       | BOSATT_I_RIKET                | 01.01.2025 | 31.08.2025 | OPPFYLT  |                              |
      | 2       | BOSATT_I_RIKET                | 01.09.2025 |            | OPPFYLT  | BOSATT_I_FINNMARK_NORD_TROMS |
      | 2       | BOR_MED_SØKER, LOVLIG_OPPHOLD | 01.01.2025 |            | OPPFYLT  |                              |

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Prosent | Ytelse type        |
      | 2       | 2            | 01.02.2025 | 30.04.2025 | 1766  | 100     | ORDINÆR_BARNETRYGD |
      | 2       | 2            | 01.05.2025 | 31.12.2042 | 1968  | 100     | ORDINÆR_BARNETRYGD |
      | 2       | 2            | 01.10.2025 | 31.12.2042 | 500   | 100     | FINNMARKSTILLEGG   |

    Så forvent følgende vedtaksperioder for behandling 2
      | Fra dato   | Til dato   | Vedtaksperiodetype | Begrunnelser               |
      | 01.09.2025 | 30.09.2025 | Utbetaling         |                            |
      | 01.10.2025 | 31.12.2042 | Utbetaling         | INNVILGET_FINNMARKSTILLEGG |
      | 01.01.2043 |            | Opphør             |                            |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.10.2025 til 31.12.2042
      | Begrunnelse                | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | INNVILGET_FINNMARKSTILLEGG | STANDARD | Ja            | 01.01.25             | 1           | september 2025                       |         | 2 468 |                  | SØKER_HAR_IKKE_RETT     |                             |

    Så forvent at brevmal AUTOVEDTAK_ENDRING er brukt for behandling 2


  Scenario: Skal oppdatere vilkårresultater og opphøre andeler når autovedtak finnmarkstillegg kjøres og man ikke lenger bor i finnmark
    Og dagens dato er 01.10.2025

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                        | Fra dato   | Til dato   | Resultat | Utdypende vilkår             |
      | 1       | BOSATT_I_RIKET                | 01.01.2025 | 30.04.2025 | OPPFYLT  |                              |
      | 1       | BOSATT_I_RIKET                | 01.05.2025 |            | OPPFYLT  | BOSATT_I_FINNMARK_NORD_TROMS |
      | 1       | LOVLIG_OPPHOLD                | 01.01.2025 |            | OPPFYLT  |                              |

      | 2       | UNDER_18_ÅR                   | 01.01.2025 | 31.12.2042 | OPPFYLT  |                              |
      | 2       | GIFT_PARTNERSKAP              | 01.01.2025 |            | OPPFYLT  |                              |
      | 2       | BOSATT_I_RIKET                | 01.01.2025 | 30.04.2025 | OPPFYLT  |                              |
      | 2       | BOSATT_I_RIKET                | 01.05.2025 |            | OPPFYLT  | BOSATT_I_FINNMARK_NORD_TROMS |
      | 2       | BOR_MED_SØKER, LOVLIG_OPPHOLD | 01.01.2025 |            | OPPFYLT  |                              |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Prosent | Ytelse type        |
      | 2       | 1            | 01.02.2025 | 30.04.2025 | 1766  | 100     | ORDINÆR_BARNETRYGD |
      | 2       | 1            | 01.05.2025 | 31.12.2042 | 1968  | 100     | ORDINÆR_BARNETRYGD |
      | 2       | 1            | 01.10.2025 | 31.12.2042 | 500   | 100     | FINNMARKSTILLEGG   |

    Og med adressekommuner
      | AktørId | Fra dato   | Til dato | Kommunenummer |
      | 1       | 01.01.2000 |          | 0301          |
      | 2       | 01.01.2025 |          | 0301          |
      | 1, 2    | 01.05.2025 |          | 5601          |
      | 2       | 15.10.2025 |          | 0301          |

    Når vi lager automatisk behandling med id 2 på fagsak 1 på grunn av finnmarkstillegg

    Så forvent følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                        | Fra dato   | Til dato   | Resultat | Utdypende vilkår             |
      | 1       | BOSATT_I_RIKET                | 01.01.2025 | 30.04.2025 | OPPFYLT  |                              |
      | 1       | BOSATT_I_RIKET                | 01.05.2025 | 31.08.2025 | OPPFYLT  |                              |
      | 1       | BOSATT_I_RIKET                | 01.09.2025 |            | OPPFYLT  | BOSATT_I_FINNMARK_NORD_TROMS |
      | 1       | LOVLIG_OPPHOLD                | 01.01.2025 |            | OPPFYLT  |                              |

      | 2       | UNDER_18_ÅR                   | 01.01.2025 | 31.12.2042 | OPPFYLT  |                              |
      | 2       | GIFT_PARTNERSKAP              | 01.01.2025 |            | OPPFYLT  |                              |
      | 2       | BOSATT_I_RIKET                | 01.01.2025 | 30.04.2025 | OPPFYLT  |                              |
      | 2       | BOSATT_I_RIKET                | 01.05.2025 | 31.08.2025 | OPPFYLT  |                              |
      | 2       | BOSATT_I_RIKET                | 01.09.2025 | 14.10.2025 | OPPFYLT  | BOSATT_I_FINNMARK_NORD_TROMS |
      | 2       | BOSATT_I_RIKET                | 15.10.2025 |            | OPPFYLT  |                              |
      | 2       | BOR_MED_SØKER, LOVLIG_OPPHOLD | 01.01.2025 |            | OPPFYLT  |                              |

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Prosent | Ytelse type        |
      | 2       | 2            | 01.02.2025 | 30.04.2025 | 1766  | 100     | ORDINÆR_BARNETRYGD |
      | 2       | 2            | 01.05.2025 | 31.12.2042 | 1968  | 100     | ORDINÆR_BARNETRYGD |
      | 2       | 2            | 01.10.2025 | 31.10.2025 | 500   | 100     | FINNMARKSTILLEGG   |

    Så forvent følgende vedtaksperioder for behandling 2
      | Fra dato   | Til dato   | Vedtaksperiodetype | Begrunnelser               |
      | 01.05.2025 | 31.08.2025 | Utbetaling         |                            |
      | 01.09.2025 | 30.09.2025 | Utbetaling         |                            |
      | 01.10.2025 | 31.10.2025 | Utbetaling         |                            |
      | 01.11.2025 | 31.12.2042 | Utbetaling         | REDUKSJON_FINNMARKSTILLEGG |
      | 01.01.2043 |            | Opphør             |                            |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.11.2025 til 31.12.2042
      | Begrunnelse                | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Beløp |
      | REDUKSJON_FINNMARKSTILLEGG | STANDARD | Nei           | 01.01.25             | 1           | oktober 2025                         | 1 968 |

  Scenario: Skal oppdatere vilkårresultater og opphøre andeler når autovedtak finnmarkstillegg kjøres og det viser seg at man aldri har bodd i finnmark
    Og dagens dato er 01.09.2025

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                        | Fra dato   | Til dato   | Resultat | Utdypende vilkår                                 |
      | 1       | BOSATT_I_RIKET                | 01.01.2025 | 31.08.2025 | OPPFYLT  |                                                  |
      | 1       | BOSATT_I_RIKET                | 01.09.2025 |            | OPPFYLT  | BOSATT_I_FINNMARK_NORD_TROMS, VURDERT_MEDLEMSKAP |
      | 1       | LOVLIG_OPPHOLD                | 01.01.2025 |            | OPPFYLT  |                                                  |

      | 2       | UNDER_18_ÅR                   | 01.01.2025 | 31.12.2042 | OPPFYLT  |                                                  |
      | 2       | GIFT_PARTNERSKAP              | 01.01.2025 |            | OPPFYLT  |                                                  |
      | 2       | BOSATT_I_RIKET                | 01.01.2025 | 31.08.2025 | OPPFYLT  |                                                  |
      | 2       | BOSATT_I_RIKET                | 01.09.2025 |            | OPPFYLT  | BOSATT_I_FINNMARK_NORD_TROMS                     |
      | 2       | BOR_MED_SØKER, LOVLIG_OPPHOLD | 01.01.2025 |            | OPPFYLT  |                                                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Prosent | Ytelse type        |
      | 2       | 1            | 01.02.2025 | 30.04.2025 | 1766  | 100     | ORDINÆR_BARNETRYGD |
      | 2       | 1            | 01.05.2025 | 31.12.2042 | 1968  | 100     | ORDINÆR_BARNETRYGD |
      | 2       | 1            | 01.10.2025 | 31.12.2042 | 500   | 100     | FINNMARKSTILLEGG   |

    Og med adressekommuner
      | AktørId | Fra dato   | Til dato | Kommunenummer |
      | 1       | 01.01.2000 |          | 0301          |
      | 2       | 01.01.2025 |          | 0301          |
      | 1       | 01.05.2025 |          | 5601          |

    Når vi lager automatisk behandling med id 2 på fagsak 1 på grunn av finnmarkstillegg

    Så forvent følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                        | Fra dato   | Til dato   | Resultat | Utdypende vilkår                                 |
      | 1       | BOSATT_I_RIKET                | 01.01.2025 | 31.08.2025 | OPPFYLT  |                                                  |
      | 1       | BOSATT_I_RIKET                | 01.09.2025 |            | OPPFYLT  | BOSATT_I_FINNMARK_NORD_TROMS, VURDERT_MEDLEMSKAP |
      | 1       | LOVLIG_OPPHOLD                | 01.01.2025 |            | OPPFYLT  |                                                  |

      | 2       | UNDER_18_ÅR                   | 01.01.2025 | 31.12.2042 | OPPFYLT  |                                                  |
      | 2       | GIFT_PARTNERSKAP              | 01.01.2025 |            | OPPFYLT  |                                                  |
      | 2       | BOSATT_I_RIKET                | 01.01.2025 | 31.08.2025 | OPPFYLT  |                                                  |
      | 2       | BOSATT_I_RIKET                | 01.09.2025 |            | OPPFYLT  |                                                  |
      | 2       | BOR_MED_SØKER, LOVLIG_OPPHOLD | 01.01.2025 |            | OPPFYLT  |                                                  |

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Prosent | Ytelse type        |
      | 2       | 2            | 01.02.2025 | 30.04.2025 | 1766  | 100     | ORDINÆR_BARNETRYGD |
      | 2       | 2            | 01.05.2025 | 31.12.2042 | 1968  | 100     | ORDINÆR_BARNETRYGD |

    Så forvent følgende vedtaksperioder for behandling 2
      | Fra dato   | Til dato   | Vedtaksperiodetype                                      | Begrunnelser                                        |
      | 01.09.2025 | 30.09.2025 | Utbetaling                                              |                                                     |
      | 01.10.2025 | 31.12.2042 | UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING | REDUKSJON_FINNMARKSTILLEGG_BODDE_IKKE_I_TILLEGGSONE |
      | 01.01.2043 |            | Opphør                                                  |                                                     |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.10.2025 til 31.12.2042
      | Begrunnelse                                         | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | REDUKSJON_FINNMARKSTILLEGG_BODDE_IKKE_I_TILLEGGSONE | STANDARD | Nei           | 01.01.25             | 1           | september 2025                       |         | 1 968 |                  | SØKER_HAR_IKKE_RETT     |                             |

  Scenario: Dersom det bare er søker som ikke bodde i finnmark, skal det bare trekkes inn søker og ikke barn
    Og dagens dato er 01.09.2025

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                        | Fra dato   | Til dato   | Resultat | Utdypende vilkår             |
      | 1       | BOSATT_I_RIKET                | 01.01.2025 | 31.08.2025 | OPPFYLT  |                              |
      | 1       | BOSATT_I_RIKET                | 01.09.2025 |            | OPPFYLT  | BOSATT_I_FINNMARK_NORD_TROMS |
      | 1       | LOVLIG_OPPHOLD                | 01.01.2025 |            | OPPFYLT  |                              |

      | 2       | UNDER_18_ÅR                   | 01.01.2025 | 31.12.2042 | OPPFYLT  |                              |
      | 2       | GIFT_PARTNERSKAP              | 01.01.2025 |            | OPPFYLT  |                              |
      | 2       | BOSATT_I_RIKET                | 01.01.2025 | 31.08.2025 | OPPFYLT  |                              |
      | 2       | BOSATT_I_RIKET                | 01.09.2025 |            | OPPFYLT  | BOSATT_I_FINNMARK_NORD_TROMS |
      | 2       | BOR_MED_SØKER, LOVLIG_OPPHOLD | 01.01.2025 |            | OPPFYLT  |                              |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Prosent | Ytelse type        |
      | 2       | 1            | 01.02.2025 | 30.04.2025 | 1766  | 100     | ORDINÆR_BARNETRYGD |
      | 2       | 1            | 01.05.2025 | 31.12.2042 | 1968  | 100     | ORDINÆR_BARNETRYGD |
      | 2       | 1            | 01.10.2025 | 31.12.2042 | 500   | 100     | FINNMARKSTILLEGG   |

    Og med adressekommuner
      | AktørId | Fra dato   | Til dato | Kommunenummer |
      | 1       | 01.01.2000 |          | 0301          |
      | 2       | 01.01.2025 |          | 0301          |
      | 2       | 01.05.2025 |          | 5601          |

    Når vi lager automatisk behandling med id 2 på fagsak 1 på grunn av finnmarkstillegg

    Så forvent følgende vilkårresultater for behandling 2
      | AktørId | Vilkår                        | Fra dato   | Til dato   | Resultat | Utdypende vilkår             |
      | 1       | BOSATT_I_RIKET                | 01.01.2025 | 31.08.2025 | OPPFYLT  |                              |
      | 1       | BOSATT_I_RIKET                | 01.09.2025 |            | OPPFYLT  |                              |
      | 1       | LOVLIG_OPPHOLD                | 01.01.2025 |            | OPPFYLT  |                              |

      | 2       | UNDER_18_ÅR                   | 01.01.2025 | 31.12.2042 | OPPFYLT  |                              |
      | 2       | GIFT_PARTNERSKAP              | 01.01.2025 |            | OPPFYLT  |                              |
      | 2       | BOSATT_I_RIKET                | 01.01.2025 | 31.08.2025 | OPPFYLT  |                              |
      | 2       | BOSATT_I_RIKET                | 01.09.2025 |            | OPPFYLT  | BOSATT_I_FINNMARK_NORD_TROMS |
      | 2       | BOR_MED_SØKER, LOVLIG_OPPHOLD | 01.01.2025 |            | OPPFYLT  |                              |

    Så forvent følgende andeler tilkjent ytelse for behandling 2
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Prosent | Ytelse type        |
      | 2       | 2            | 01.02.2025 | 30.04.2025 | 1766  | 100     | ORDINÆR_BARNETRYGD |
      | 2       | 2            | 01.05.2025 | 31.12.2042 | 1968  | 100     | ORDINÆR_BARNETRYGD |

    Så forvent følgende vedtaksperioder for behandling 2
      | Fra dato   | Til dato   | Vedtaksperiodetype                                      | Begrunnelser                                        |
      | 01.09.2025 | 30.09.2025 | Utbetaling                                              |                                                     |
      | 01.10.2025 | 31.12.2042 | UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING | REDUKSJON_FINNMARKSTILLEGG_BODDE_IKKE_I_TILLEGGSONE |
      | 01.01.2043 |            | Opphør                                                  |                                                     |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.10.2025 til 31.12.2042
      | Begrunnelse                                         | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | REDUKSJON_FINNMARKSTILLEGG_BODDE_IKKE_I_TILLEGGSONE | STANDARD | Ja            |                      | 0           | september 2025                       |         | 1 968 |                  | SØKER_HAR_IKKE_RETT     |                             |