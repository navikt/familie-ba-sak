# language: no
# encoding: UTF-8

Egenskap: Reduksjon på svalbardtillegg

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype | Status  |
      | 1        | NORMAL     | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | NASJONAL            | AVSLUTTET         |
      | 2            | 1        | 1                   | ENDRET_UTBETALING   | NYE_OPPLYSNINGER | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 26.12.1992  |              |
      | 1            | 2       | BARN       | 14.06.2011  |              |
      | 2            | 1       | SØKER      | 26.12.1992  |              |
      | 2            | 2       | BARN       | 14.06.2011  |              |

  Scenario: Skal få opp reduksjonsbegrunnelse for Svalbard når søker flytter ut av Svalbard
    Og dagens dato er 17.09.2025

    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                       | Utdypende vilkår   | Fra dato   | Til dato   | Resultat |
      | 1       | LOVLIG_OPPHOLD               |                    | 26.12.1992 |            | OPPFYLT  |
      | 1       | BOSATT_I_RIKET               | BOSATT_PÅ_SVALBARD | 14.06.2011 |            | OPPFYLT  |

      | 2       | BOR_MED_SØKER,LOVLIG_OPPHOLD |                    | 14.06.2011 |            | OPPFYLT  |
      | 2       | GIFT_PARTNERSKAP             |                    | 14.06.2011 |            | OPPFYLT  |
      | 2       | BOSATT_I_RIKET               | BOSATT_PÅ_SVALBARD | 14.06.2011 |            | OPPFYLT  |
      | 2       | UNDER_18_ÅR                  |                    | 14.06.2011 | 13.06.2029 | OPPFYLT  |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår   | Fra dato   | Til dato   | Resultat |
      | 1       | LOVLIG_OPPHOLD               |                    | 26.12.1992 |            | OPPFYLT  |
      | 1       | BOSATT_I_RIKET               | BOSATT_PÅ_SVALBARD | 14.06.2011 | 08.08.2025 | OPPFYLT  |
      | 1       | BOSATT_I_RIKET               |                    | 09.08.2025 |            | OPPFYLT  |

      | 2       | GIFT_PARTNERSKAP             |                    | 14.06.2011 |            | OPPFYLT  |
      | 2       | UNDER_18_ÅR                  |                    | 14.06.2011 | 13.06.2029 | OPPFYLT  |
      | 2       | BOR_MED_SØKER,LOVLIG_OPPHOLD |                    | 14.06.2011 |            | OPPFYLT  |
      | 2       | BOSATT_I_RIKET               | BOSATT_PÅ_SVALBARD | 14.06.2011 |            | OPPFYLT  |


    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.05.2025 | 31.05.2029 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 2       | 1            | 01.08.2025 | 31.05.2029 | 500   | SVALBARDTILLEGG    | 100     | 500  |

      | 2       | 2            | 01.05.2025 | 31.05.2029 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 2       | 2            | 01.08.2025 | 31.08.2025 | 500   | SVALBARDTILLEGG    | 100     | 500  |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser      | Ugyldige begrunnelser |
      | 01.09.2025 | 31.05.2029 | UTBETALING         |                                | REDUKSJON_SVALBARDTILLEGG |                       |
      | 01.06.2029 |            | OPPHØR             |                                |                           |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser      | Eøsbegrunnelser | Fritekster |
      | 01.09.2025 | 31.05.2029 | REDUKSJON_SVALBARDTILLEGG |                 |            |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 2 i periode 01.09.2025 til 31.05.2029
      | Begrunnelse               | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | Avtaletidspunkt delt bosted |
      | REDUKSJON_SVALBARDTILLEGG | STANDARD | Ja            |                      | 0           | august 2025                          |         | 1 968 |                  | SØKER_HAR_IKKE_RETT     |                             |

