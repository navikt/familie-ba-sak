# language: no
# encoding: UTF-8

Egenskap: Innvilget finnmarkstillegg institusjon

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype  | Status  |
      | 1        | INSTITUSJON | LØPENDE |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori | Behandlingsstatus |
      | 1            | 1        |                     | ENDRET_UTBETALING   | NYE_OPPLYSNINGER | Nei                       | NASJONAL            | UTREDES           |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 2       | BARN       | 05.08.2015  |
      | 1            | 2       | BARN       | 05.08.2015  |

  Scenario: Dersom det innvilges Finnmarkstillegg fordi institusjon ligger i Finnmark skal innvilgetFinnmarkstilleggInstitusjon-begrunnelse være gyldig
    Og dagens dato er 23.09.2025

    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat |
      | 2       | UNDER_18_ÅR      |                              | 05.08.2015 | 04.08.2033 | OPPFYLT  |
      | 2       | BOSATT_I_RIKET   |                              | 05.08.2015 | 14.07.2025 | OPPFYLT  |
      | 2       | GIFT_PARTNERSKAP |                              | 05.08.2015 |            | OPPFYLT  |
      | 2       | BOR_MED_SØKER    |                              | 05.08.2015 |            | OPPFYLT  |
      | 2       | LOVLIG_OPPHOLD   |                              | 05.08.2015 |            | OPPFYLT  |
      | 2       | BOSATT_I_RIKET   | BOSATT_I_FINNMARK_NORD_TROMS | 15.07.2025 |            | OPPFYLT  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.06.2025 | 31.07.2033 | 1968  | ORDINÆR_BARNETRYGD | 100     | 1968 |
      | 2       | 1            | 01.08.2025 | 31.10.2042 | 500   | FINNMARKSTILLEGG   | 100     | 500  |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Gyldige begrunnelser                   | Ugyldige begrunnelser                |
      | 01.08.2025 | 31.07.2033 | UTBETALING         | INNVILGET_FINNMARKSTILLEGG_INSTITUSJON | INNVILGET_FINNMARKSTILLEGG_UTEN_DATO |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                   |
      | 01.08.2025 | 31.07.2033 | INNVILGET_FINNMARKSTILLEGG_INSTITUSJON |

    Så forvent følgende brevbegrunnelser i rekkefølge for behandling 1 i periode 01.08.2025 til 31.07.2033
      | Begrunnelse                            | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Beløp |
      | INNVILGET_FINNMARKSTILLEGG_INSTITUSJON | Nei           | 05.08.15             | 1           | juli 2025                            | 2 468 |                             
