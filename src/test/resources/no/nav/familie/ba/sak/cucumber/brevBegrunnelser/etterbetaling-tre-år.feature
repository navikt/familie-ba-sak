# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser for endret utbetaling med etterbetaling tre år tilbake i tid

  Bakgrunn:
    Gitt følgende behandling
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 29.08.1986  |
      | 1            | 2       | BARN       | 05.02.2005  |
      | 1            | 3       | BARN       | 03.11.2010  |

  Scenario: Skal kunne begrunne utvidet for to barn etter endret utbetaling med etterbetaling tre år tilbake i tid
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                        | Fra dato   | Til dato   | Resultat | Utdypende vilkår |
      | 1       | BOSATT_I_RIKET, LOVLIG_OPPHOLD                | 01.01.2020 | 28.02.2023 | Oppfylt  |                  |
      | 1       | UTVIDET_BARNETRYGD                            | 01.01.2020 | 20.06.2022 | Oppfylt  |                  |
      | 2       | UNDER_18_ÅR                                   | 05.02.2005 | 04.02.2023 | Oppfylt  |                  |
      | 2       | GIFT_PARTNERSKAP                              | 05.02.2005 |            | Oppfylt  |                  |
      | 2       | BOR_MED_SØKER, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 01.01.2020 |            | Oppfylt  |                  |
      | 3       | UNDER_18_ÅR                                   | 03.11.2010 | 02.11.2028 | Oppfylt  |                  |
      | 3       | GIFT_PARTNERSKAP                              | 03.11.2010 |            | Oppfylt  |                  |
      | 3       | BOR_MED_SØKER                                 | 01.01.2020 |            | Oppfylt  | DELT_BOSTED      |
      | 3       | BOSATT_I_RIKET, LOVLIG_OPPHOLD                | 01.01.2020 |            | Oppfylt  |                  |

    Og med andeler tilkjent ytelse for begrunnelse
      | BehandlingId | AktørId | Fra dato   | Til dato   | Beløp |
      | 1            | 1       | 01.02.2020 | 31.05.2020 | 0     |
      | 1            | 1       | 01.06.2020 | 30.06.2022 | 1054  |
      | 1            | 2       | 01.02.2020 | 31.01.2023 | 1054  |
      | 1            | 3       | 01.02.2020 | 28.02.2023 | 527   |

    Og med endrede utbetalinger for begrunnelse
      | BehandlingId | AktørId | Fra dato   | Til dato   | Årsak             | Prosent |
      | 1            | 1       | 01.02.2020 | 01.05.2020 | ETTERBETALING_3ÅR | 0       |

    Og med vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Standardbegrunnelser                                                        | Eøsbegrunnelser | Fritekster |
      | 01.06.2020 | 30.06.2022 | INNVILGET_BOR_ALENE_MED_BARN, ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_AAR |                 |            |
      | 01.07.2022 | 31.01.2023 | REDUKSJON_SAMBOER_MER_ENN_12_MÅNEDER                                        |                 |            |
      | 01.02.2023 | 28.02.2023 | REDUKSJON_UNDER_18_ÅR                                                       |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.06.2020 til 30.06.2022
      | Begrunnelse                                   | Gjelder søker | Barnas fødselsdatoer | Antall barn med utbetaling | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | INNVILGET_BOR_ALENE_MED_BARN                  | Ja            | 05.02.05 og 03.11.10 | 2                          | mai 2020                             | NB      | 2635  | 30.06.23         | SØKER_FÅR_UTVIDET       |
      | ETTER_ENDRET_UTBETALING_ETTERBETALING_TRE_AAR | Ja            | 05.02.05 og 03.11.10 | 2                          | mai 2020                             | NB      | 2635  | 30.06.23         | SØKER_FÅR_UTVIDET       |

