# language: no
# encoding: UTF-8

Egenskap: Plassholdertekst for egenskap - JezmCIgDYB

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId  | Fagsaktype |
      | 200061001 | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId  | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk |
      | 100179702    | 200061001 |                     | INNVILGET           | SØKNAD           | Nei                       |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId       | Persontype | Fødselsdato |
      | 100179702    | 2774626965456 | BARN       | 16.03.2022  |
      | 100179702    | 2921636519855 | SØKER      | 28.07.1985  |

  Scenario: Plassholdertekst for scenario - 6K0EsEXcJx
    Og følgende dagens dato 12.10.2023
    Og lag personresultater for begrunnelse for behandling 100179702

    Og legg til nye vilkårresultater for begrunnelse for behandling 100179702
      | AktørId       | Vilkår                          | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 2774626965456 | BOR_MED_SØKER                   | BARN_BOR_I_EØS_MED_SØKER     | 16.03.2022 |            | OPPFYLT  | Nei                  |
      | 2774626965456 | UNDER_18_ÅR                     |                              | 16.03.2022 | 15.03.2040 | OPPFYLT  | Nei                  |
      | 2774626965456 | BOSATT_I_RIKET                  | BARN_BOR_I_NORGE             | 16.03.2022 |            | OPPFYLT  | Nei                  |
      | 2774626965456 | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD |                              | 16.03.2022 |            | OPPFYLT  | Nei                  |

      | 2921636519855 | LOVLIG_OPPHOLD                  |                              | 28.07.1985 |            | OPPFYLT  | Nei                  |
      | 2921636519855 | BOSATT_I_RIKET                  | OMFATTET_AV_NORSK_LOVGIVNING | 28.07.1985 |            | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId       | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2774626965456 | 100179702    | 01.04.2022 | 28.02.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 2774626965456 | 100179702    | 01.03.2023 | 30.06.2023 | 0     | ORDINÆR_BARNETRYGD | 100     | 1723 |
      | 2774626965456 | 100179702    | 01.07.2023 | 29.02.2028 | 23    | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 2774626965456 | 100179702    | 01.03.2028 | 29.02.2040 | 0     | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med kompetanser for begrunnelse
      | AktørId       | Fra dato   | Til dato | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2774626965456 | 01.04.2022 |          | NORGE_ER_SEKUNDÆRLAND | 100179702    | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |

    Når begrunnelsetekster genereres for behandling 100179702

    Så forvent følgende standardBegrunnelser
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk        | Inkluderte Begrunnelser                                                 | Ekskluderte Begrunnelser |
      | 01.04.2022 | 30.06.2023 | UTBETALING         |                  |                                                                         |                          |
      | 01.04.2022 | 30.06.2023 | UTBETALING         | EØS_FORORDNINGEN | INNVILGET_SEKUNDÆRLAND_STANDARD, INNVILGET_TILLEGGSTEKST_NULLUTBETALING |                          |
      | 01.07.2023 | 29.02.2028 | UTBETALING         |                  | INNVILGET_SATSENDRING                                                   |                          |
      | 01.03.2028 | 29.02.2040 | UTBETALING         |                  |                                                                         |                          |
      | 01.03.2040 |            | OPPHØR             |                  |                                                                         |                          |
    Og med vedtaksperioder for behandling 100179702
      | Fra dato   | Til dato   | Standardbegrunnelser  | Eøsbegrunnelser                                                         | Fritekster |
      | 01.04.2022 | 30.06.2023 |                       | INNVILGET_SEKUNDÆRLAND_STANDARD, INNVILGET_TILLEGGSTEKST_NULLUTBETALING |            |
      | 01.07.2023 | 29.02.2028 | INNVILGET_SATSENDRING |                                                                         |            |
      | 01.03.2028 | 29.02.2040 |                       |                                                                         |            |
      | 01.03.2040 |            |                       |                                                                         |            |
    Så forvent følgende brevperioder for behandling 100179702
      | Brevperiodetype | Fra dato   | Til dato   | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |
      |                 | 01.04.2022 | 30.06.2023 |       |                            |                     |                        |
      |                 | 01.07.2023 | 29.02.2028 |       |                            |                     |                        |
      |                 | 01.03.2028 | 29.02.2040 |       |                            |                     |                        |
      |                 | 01.03.2040 |            |       |                            |                     |                        |

    Så forvent følgende brevbegrunnelser for behandling 100179702 i periode 01.04.2022 til 30.06.2023
      | Begrunnelse                            | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | [
      | INNVILGET_SEKUNDÆRLAND_STANDARD        |               |                      |             |                                      |         |       |                  |                         | ,
      | INNVILGET_TILLEGGSTEKST_NULLUTBETALING |               |                      |             |                                      |         |       |                  |                         | ]

    Så forvent følgende brevbegrunnelser for behandling 100179702 i periode 01.07.2023 til 29.02.2028
      | Begrunnelse           | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | [
      | INNVILGET_SATSENDRING |               |                      |             |                                      |         |       |                  |                         | ]

    Så forvent følgende brevbegrunnelser for behandling 100179702 i periode 01.03.2028 til 29.02.2040
      | Begrunnelse | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | []

    Så forvent følgende brevbegrunnelser for behandling 100179702 i periode 01.03.2040 til -
      | Begrunnelse | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet | []