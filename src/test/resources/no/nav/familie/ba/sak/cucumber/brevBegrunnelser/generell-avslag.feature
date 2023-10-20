# language: no
# encoding: UTF-8

Egenskap: Plassholdertekst for egenskap - U6VDnJbP80

  Bakgrunn:
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | AVSLÅTT             | SØKNAD           | Nei                       | NASJONAL            |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 09.04.1988  |
      | 1            | 2       | BARN       | 14.11.2012  |

  Scenario: Plassholdertekst for scenario - qFEtnodgwb
    Og følgende dagens dato 20.10.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser                    |
      | 1       | LOVLIG_OPPHOLD   |                  |            |            | IKKE_OPPFYLT | Ja                   | AVSLAG_LOVLIG_OPPHOLD_TREDJELANDSBORGER |
      | 1       | BOSATT_I_RIKET   |                  | 09.04.1988 |            | OPPFYLT      | Nei                  |                                         |

      | 2       | LOVLIG_OPPHOLD   |                  |            |            | IKKE_OPPFYLT | Ja                   | AVSLAG_LOVLIG_OPPHOLD_TREDJELANDSBORGER |
      | 2       | UNDER_18_ÅR      |                  | 14.11.2012 | 13.11.2030 | OPPFYLT      | Nei                  |                                         |
      | 2       | GIFT_PARTNERSKAP |                  | 14.11.2012 |            | OPPFYLT      | Nei                  |                                         |
      | 2       | BOSATT_I_RIKET   |                  | 14.11.2012 |            | OPPFYLT      | Nei                  |                                         |
      | 2       | BOR_MED_SØKER    |                  | 03.10.2023 |            | IKKE_OPPFYLT | Ja                   | AVSLAG_BOR_HOS_SØKER                    |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato | Til dato | Beløp | Ytelse type | Prosent | Sats |


    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                                          | Ugyldige begrunnelser |
      |            | 31.10.2023 | AVSLAG             |                                | AVSLAG_LOVLIG_OPPHOLD_TREDJELANDSBORGER                       |                       |
      |            |            | AVSLAG             |                                | AVSLAG_LOVLIG_OPPHOLD_TREDJELANDSBORGER                       |                       |
      | 01.11.2023 |            | AVSLAG             |                                | AVSLAG_BOR_HOS_SØKER, AVSLAG_LOVLIG_OPPHOLD_TREDJELANDSBORGER |                       |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                                          | Eøsbegrunnelser | Fritekster |
      |            | 31.10.2023 | AVSLAG_LOVLIG_OPPHOLD_TREDJELANDSBORGER                       |                 |            |
      |            |            | AVSLAG_LOVLIG_OPPHOLD_TREDJELANDSBORGER                       |                 |            |
      | 01.11.2023 |            | AVSLAG_BOR_HOS_SØKER, AVSLAG_LOVLIG_OPPHOLD_TREDJELANDSBORGER |                 |            |

    Så forvent følgende brevperioder for behandling 1
      | Brevperiodetype | Fra dato      | Til dato     | Beløp | Antall barn med utbetaling | Barnas fødselsdager | Du eller institusjonen |
      |                 |               | oktober 2023 |       |                            |                     |                        |
      |                 |               |              |       |                            |                     |                        |
      |                 | november 2023 |              |       |                            |                     |                        |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode - til 31.10.2023
      | Begrunnelse                             | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | AVSLAG_LOVLIG_OPPHOLD_TREDJELANDSBORGER | STANDARD |               |                      |             |                                      |         |       |                  |                         |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode - til -
      | Begrunnelse                             | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | AVSLAG_LOVLIG_OPPHOLD_TREDJELANDSBORGER | STANDARD |               |                      |             |                                      |         |       |                  |                         |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.11.2023 til -
      | Begrunnelse                             | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | AVSLAG_BOR_HOS_SØKER                    | STANDARD |               |                      |             |                                      |         |       |                  |                         |
      | AVSLAG_LOVLIG_OPPHOLD_TREDJELANDSBORGER | STANDARD |               |                      |             |                                      |         |       |                  |                         |
