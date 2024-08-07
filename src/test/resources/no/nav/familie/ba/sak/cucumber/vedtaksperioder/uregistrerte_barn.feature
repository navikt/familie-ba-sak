# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder for behandling med uregistrert barn

  Bakgrunn:
    Gitt følgende behandlinger
      | BehandlingId |
      | 1            |

  Scenario: Skal lage avslagsperiode uten datoer når vi har et uregistrert barn

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                         | Fra dato   | Til dato | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD | 11.01.1970 |          | Oppfylt  |

    Og med uregistrerte barn

    Når vedtaksperiodene genereres for behandling 1

    Så forvent følgende vedtaksperioder for behandling 1
      | Fra dato | Til dato | Vedtaksperiodetype | Kommentar | Begrunnelser            |
      |          |          | Avslag             |           | AVSLAG_UREGISTRERT_BARN |

  Scenario: Skal lage avslagsperiode uten datoer når vi har uregistrert barn og barn med eksplistt avslag

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 02.12.2016  |

    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 1            | 3456    |

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt      |                      |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER |            |            | Ikke_oppfylt | Ja                   |
      | 3456    | UNDER_18_ÅR                                                     | 02.12.2016 | 01.12.2034 | Oppfylt      |                      |

    Og med uregistrerte barn

    Når vedtaksperiodene genereres for behandling 1

    Så forvent følgende vedtaksperioder for behandling 1
      | Fra dato | Til dato | Vedtaksperiodetype | Kommentar | Begrunnelser            |
      |          |          | Avslag             |           | AVSLAG_UREGISTRERT_BARN |

  Scenario: Skal lage avslagsperiode uten datoer når vi har uregistrert barn og et barn med utbetaling

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 02.12.2016  |

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                          | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                                  | 11.01.1970 |            | Oppfylt  |                      |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD, BOR_MED_SØKER | 02.12.2016 |            | Oppfylt  |                      |
      | 3456    | UNDER_18_ÅR                                                     | 02.12.2016 | 01.12.2034 | Oppfylt  |                      |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.01.2017 | 30.11.2034 | 1234  | 1            |

    Og med uregistrerte barn

    Når vedtaksperiodene genereres for behandling 1

    Så forvent følgende vedtaksperioder for behandling 1
      | Fra dato   | Til dato   | Vedtaksperiodetype | Kommentar | Begrunnelser            |
      | 01.01.2017 | 30.11.2034 | Utbetaling         |           |                         |
      | 01.12.2034 |            | Opphør             |           |                         |
      |            |            | Avslag             |           | AVSLAG_UREGISTRERT_BARN |

  Scenario: Skal lage avslagsperiode som begrunner eksplisitt avslag i søkers vilkår dersom det bare finnes uregistrert barn

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår         | Fra dato   | Til dato | Resultat     | Er eksplisitt avslag |
      | 1234    | BOSATT_I_RIKET | 11.01.1970 |          | Oppfylt      |                      |
      | 1234    | LOVLIG_OPPHOLD | 11.01.1970 |          | Ikke_oppfylt | Ja                   |

    Og med uregistrerte barn

    Når vedtaksperiodene genereres for behandling 1

    Så forvent følgende vedtaksperioder for behandling 1
      | Fra dato   | Til dato | Vedtaksperiodetype | Kommentar | Begrunnelser            |
      | 01.02.1970 |          | Avslag             |           |                         |
      |            |          | Avslag             |           | AVSLAG_UREGISTRERT_BARN |

  Scenario: Skal ikke inkludere uregistrerte barn i begrunnelser som ikke er AVSLAG_UREGISTRERT_BARN
    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |
      | 1            | 3456    | BARN       | 13.04.2020  |

    Og lag personresultater for behandling 1

    Og med uregistrerte barn

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                           | Fra dato   | Til dato   | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD                   | 11.01.1970 |            | Oppfylt  |
      | 3456    | UNDER_18_ÅR                                      | 13.04.2020 | 12.04.2038 | Oppfylt  |
      | 3456    | GIFT_PARTNERSKAP, BOSATT_I_RIKET, LOVLIG_OPPHOLD | 13.04.2020 |            | Oppfylt  |
      | 3456    | BOR_MED_SØKER                                    | 13.04.2020 | 10.03.2021 | Oppfylt  |

    Og med andeler tilkjent ytelse
      | AktørId | Fra dato   | Til dato   | Beløp | BehandlingId |
      | 3456    | 01.05.2020 | 31.03.2021 | 1354  | 1            |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato | Standardbegrunnelser          | Eøsbegrunnelser | Fritekster |
      | 01.04.2021 |          | OPPHØR_BARN_FLYTTET_FRA_SØKER |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.04.2021 til -
      | Begrunnelse                   | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | OPPHØR_BARN_FLYTTET_FRA_SØKER | Nei           | 13.04.20             | 1           | mars 2021                            | NB      | 0     |                  | SØKER_HAR_IKKE_RETT     |
