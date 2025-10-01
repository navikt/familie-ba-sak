# language: no
# encoding: UTF-8

Egenskap: Innvilget Finnmarkstillegg begrunnelse pga søker

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | ENDRET_UTBETALING   | NYE_OPPLYSNINGER | Nei                       | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | NYE_OPPLYSNINGER | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 15.05.1985  |
      | 1            | 2       | BARN       | 05.08.2015  |
      | 1            | 3       | BARN       | 10.11.2024  |
      | 2            | 1       | SØKER      | 15.05.1985  |
      | 2            | 2       | BARN       | 05.08.2015  |
      | 2            | 3       | BARN       | 10.11.2024  |

  Scenario: Dersom det innvilges Finnmarkstillegg fordi søker flytter til Finnmark i ny behandling skal innvilgetFinnmarkstillegg-begrunnelse være gyldig
    Og dagens dato er 23.09.2025

    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår             | Fra dato   | Til dato   | Resultat |
      | 1       | LOVLIG_OPPHOLD               |                              | 15.05.1985 |            | OPPFYLT  |
      | 1       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 05.08.2015 | 14.07.2025 | OPPFYLT  |
      | 1       | BOSATT_I_RIKET               |                              | 15.07.2025 |            | OPPFYLT  |

      | 2       | GIFT_PARTNERSKAP             |                              | 05.08.2015 |            | OPPFYLT  |
      | 2       | LOVLIG_OPPHOLD               |                              | 05.08.2015 |            | OPPFYLT  |
      | 2       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 05.08.2015 |            | OPPFYLT  |
      | 2       | BOR_MED_SØKER                |                              | 05.08.2015 |            | OPPFYLT  |
      | 2       | UNDER_18_ÅR                  |                              | 05.08.2015 | 04.08.2033 | OPPFYLT  |

      | 3       | GIFT_PARTNERSKAP             |                              | 10.11.2024 |            | OPPFYLT  |
      | 3       | LOVLIG_OPPHOLD,BOR_MED_SØKER |                              | 10.11.2024 |            | OPPFYLT  |
      | 3       | UNDER_18_ÅR                  |                              | 10.11.2024 | 09.11.2042 | OPPFYLT  |
      | 3       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 10.11.2024 |            | OPPFYLT  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår             | Fra dato   | Til dato   | Resultat |
      | 1       | LOVLIG_OPPHOLD               |                              | 15.05.1985 |            | OPPFYLT  |
      | 1       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 05.08.2015 |            | OPPFYLT  |

      | 2       | UNDER_18_ÅR                  |                              | 05.08.2015 | 04.08.2033 | OPPFYLT  |
      | 2       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 05.08.2015 | 14.07.2025 | OPPFYLT  |
      | 2       | GIFT_PARTNERSKAP             |                              | 05.08.2015 |            | OPPFYLT  |
      | 2       | BOR_MED_SØKER                |                              | 05.08.2015 |            | OPPFYLT  |
      | 2       | LOVLIG_OPPHOLD               |                              | 05.08.2015 |            | OPPFYLT  |
      | 2       | BOSATT_I_RIKET               |                              | 15.07.2025 |            | OPPFYLT  |

      | 3       | GIFT_PARTNERSKAP             |                              | 10.11.2024 |            | OPPFYLT  |
      | 3       | BOR_MED_SØKER,LOVLIG_OPPHOLD |                              | 10.11.2024 |            | OPPFYLT  |
      | 3       | UNDER_18_ÅR                  |                              | 10.11.2024 | 09.11.2042 | OPPFYLT  |
      | 3       | BOSATT_I_RIKET               | BOSATT_I_FINNMARK_NORD_TROMS | 10.11.2024 |            | OPPFYLT  |


    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |

      | 2       | 1            | 01.06.2025 | 31.07.2033 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 1            | 01.12.2024 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 3       | 1            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 3       | 1            | 01.06.2025 | 31.10.2042 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |


      | 2       | 2            | 01.06.2025 | 31.07.2033 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 2            | 01.12.2024 | 30.04.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1766 |
      | 3       | 2            | 01.05.2025 | 31.05.2025 | 0     | ORDINÆR_BARNETRYGD | 0       | 1968 |
      | 3       | 2            | 01.06.2025 | 31.10.2042 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 3       | 2            | 01.08.2025 | 31.10.2042 | 500   | FINNMARKSTILLEGG   | 100     | 500  |

    Når vedtaksperiodene genereres for behandling 2


    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser       | Ugyldige begrunnelser |
      | 01.08.2025 | 31.07.2033 | UTBETALING         |                                | INNVILGET_FINNMARKSTILLEGG |                       |
      | 01.08.2025 | 31.07.2033 | UTBETALING         |                                | INNVILGET_BOSATT_I_RIKET   |                       |


    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser                                 | Eøsbegrunnelser | Fritekster |
      | 01.08.2025 | 31.07.2033 | INNVILGET_FINNMARKSTILLEGG, INNVILGET_BOSATT_I_RIKET |                 |            |


    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.08.2025 til 31.07.2033
      | Begrunnelse                | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | INNVILGET_BOSATT_I_RIKET   | STANDARD | Nei           | 05.08.15             | 1           | juli 2025                            |         | 1 968 |                  | SØKER_HAR_IKKE_RETT     |                             |
      | INNVILGET_FINNMARKSTILLEGG | STANDARD | Ja            | 10.11.24             | 1           | juli 2025                            |         | 4 436 |                  | SØKER_HAR_IKKE_RETT     |                             |


