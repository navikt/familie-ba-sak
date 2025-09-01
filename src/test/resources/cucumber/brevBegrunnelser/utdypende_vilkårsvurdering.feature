# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser ved endring av utdypende vilkår

  Bakgrunn:

  Scenario: Skal flette inn alle barna ved overgang til bosatt i riket med vurdert medlemskap for søker
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk |
      | 1            | 1        |                     | INNVILGET           | SØKNAD           | Nei                       |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 02.10.1970  |
      | 1            | 2       | BARN       | 03.12.2007  |
      | 1            | 3       | BARN       | 21.07.2014  |

    Og dagens dato er 16.10.2023
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår   | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD                                               |                    | 02.10.1970 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                                               |                    | 15.06.2023 | 31.07.2023 | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                                               | VURDERT_MEDLEMSKAP | 01.08.2023 |            | OPPFYLT  | Nei                  |

      | 2       | BOR_MED_SØKER,GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                    | 03.12.2007 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                                  |                    | 03.12.2007 | 02.12.2025 | OPPFYLT  | Nei                  |

      | 3       | UNDER_18_ÅR                                                  |                    | 21.07.2014 | 20.07.2032 | OPPFYLT  | Nei                  |
      | 3       | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOR_MED_SØKER                |                    | 21.07.2014 |            | OPPFYLT  | Nei                  |
      | 3       | BOSATT_I_RIKET                                               | VURDERT_MEDLEMSKAP | 21.07.2014 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.07.2023 | 30.11.2025 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 1            | 01.07.2023 | 30.06.2032 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk | Gyldige begrunnelser                               | Ugyldige begrunnelser |
      | 01.08.2023 | 30.11.2025 | UTBETALING         |           | INNVILGET_VURDERING_HELE_FAMILIEN_FRIVILLIG_MEDLEM |                       |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                               | Eøsbegrunnelser | Fritekster |
      | 01.08.2023 | 30.11.2025 | INNVILGET_VURDERING_HELE_FAMILIEN_FRIVILLIG_MEDLEM |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.08.2023 til 30.11.2025
      | Begrunnelse                                        | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp |
      | INNVILGET_VURDERING_HELE_FAMILIEN_FRIVILLIG_MEDLEM | STANDARD | Ja            | 03.12.07 og 21.07.14 | 2           | juli 2023                            | NB      | 2 620 |