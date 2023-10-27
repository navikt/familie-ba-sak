# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser ved eksplisitte avslag vs avslag

  Bakgrunn:

  Scenario: Når det er et avslag samtidig som et eksplisitt avslag skal kun barnet med eksplisitt avslag flettes inn i begrunnelsen
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat         | Behandlingsårsak | Skal behandles automatisk |
      | 1            | 1        |                     | DELVIS_INNVILGET_OG_OPPHØRT | SØKNAD           | Nei                       |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 14.10.1987  |
      | 1            | 3456    | BARN       | 13.01.2017  |
      | 1            | 5678    | BARN       | 08.02.2013  |

    Og følgende dagens dato 12.10.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser |
      | 1234    | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 05.05.2022 |            | OPPFYLT      | Nei                  |                      |

      | 5678    | UNDER_18_ÅR                   |                  | 08.02.2013 | 07.02.2031 | OPPFYLT      | Nei                  |                      |
      | 5678    | GIFT_PARTNERSKAP              |                  | 08.02.2013 |            | OPPFYLT      | Nei                  |                      |
      | 5678    | BOR_MED_SØKER                 |                  | 08.02.2013 | 02.03.2023 | OPPFYLT      | Nei                  |                      |
      | 5678    | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 05.05.2022 |            | OPPFYLT      | Nei                  |                      |
      | 5678    | BOR_MED_SØKER                 |                  | 03.03.2023 |            | IKKE_OPPFYLT | Nei                  |                      |

      | 3456    | UNDER_18_ÅR                   |                  | 13.01.2017 | 12.01.2035 | OPPFYLT      | Nei                  |                      |
      | 3456    | GIFT_PARTNERSKAP              |                  | 13.01.2017 |            | OPPFYLT      | Nei                  |                      |
      | 3456    | BOR_MED_SØKER                 |                  | 13.01.2017 | 02.03.2023 | OPPFYLT      | Nei                  |                      |
      | 3456    | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 05.05.2022 |            | OPPFYLT      | Nei                  |                      |
      | 3456    | BOR_MED_SØKER                 |                  | 03.03.2023 |            | IKKE_OPPFYLT | Ja                   | AVSLAG_BOR_HOS_SØKER |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 5678    | 1            | 01.06.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5678    | 1            | 01.03.2023 | 31.03.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3456    | 1            | 01.06.2022 | 31.12.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3456    | 1            | 01.01.2023 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3456    | 1            | 01.03.2023 | 31.03.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato | Standardbegrunnelser | Eøsbegrunnelser | Fritekster |
      | 01.04.2023 |          | AVSLAG_BOR_HOS_SØKER |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.04.2023 til -
      | Begrunnelse          | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søknadstidspunkt | Søkers rett til utvidet |
      | AVSLAG_BOR_HOS_SØKER | STANDARD | Nei           | 13.01.17             | 1           | mars 2023                            | NB      | 0     |                  | SØKER_HAR_IKKE_RETT     |


  Scenario: Når det er et avslag samtidig som et eksplisitt avslag og det ikke er valgt noen begrunnelse for avslaget skal vi ikke få opp noen avslagsbegrunnelse
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat         | Behandlingsårsak | Skal behandles automatisk |
      | 1            | 1        |                     | DELVIS_INNVILGET_OG_OPPHØRT | SØKNAD           | Nei                       |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 14.10.1987  |
      | 1            | 3456    | BARN       | 13.01.2017  |
      | 1            | 5678    | BARN       | 08.02.2013  |

    Og følgende dagens dato 12.10.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser |
      | 1234    | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 05.05.2022 |            | OPPFYLT      | Nei                  |                      |

      | 5678    | UNDER_18_ÅR                   |                  | 08.02.2013 | 07.02.2031 | OPPFYLT      | Nei                  |                      |
      | 5678    | GIFT_PARTNERSKAP              |                  | 08.02.2013 |            | OPPFYLT      | Nei                  |                      |
      | 5678    | BOR_MED_SØKER                 |                  | 08.02.2013 | 02.03.2023 | OPPFYLT      | Nei                  |                      |
      | 5678    | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 05.05.2022 |            | OPPFYLT      | Nei                  |                      |
      | 5678    | BOR_MED_SØKER                 |                  | 03.03.2023 |            | IKKE_OPPFYLT | Nei                  |                      |

      | 3456    | UNDER_18_ÅR                   |                  | 13.01.2017 | 12.01.2035 | OPPFYLT      | Nei                  |                      |
      | 3456    | GIFT_PARTNERSKAP              |                  | 13.01.2017 |            | OPPFYLT      | Nei                  |                      |
      | 3456    | BOR_MED_SØKER                 |                  | 13.01.2017 | 02.03.2023 | OPPFYLT      | Nei                  |                      |
      | 3456    | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 05.05.2022 |            | OPPFYLT      | Nei                  |                      |
      | 3456    | BOR_MED_SØKER                 |                  | 03.03.2023 |            | IKKE_OPPFYLT | Ja                   |                      |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 5678    | 1            | 01.06.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 5678    | 1            | 01.03.2023 | 31.03.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3456    | 1            | 01.06.2022 | 31.12.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3456    | 1            | 01.01.2023 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3456    | 1            | 01.03.2023 | 31.03.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |

    Når vedtaksperiodene genereres for behandling 1

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato | VedtaksperiodeType | Gyldige begrunnelser | Ugyldige begrunnelser |
      | 01.04.2023 |          | OPPHØR             |                      | AVSLAG_BOR_HOS_SØKER  |


  Scenario: Skal ta med barna med avslag når det er avslag på søker og barn
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | AVSLÅTT             | SØKNAD           | Nei                       | NASJONAL            |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 30.07.1983  |
      | 1            | 2       | BARN       | 28.07.2012  |
      | 1            | 3       | BARN       | 24.06.2014  |
      | 1            | 4       | BARN       | 09.05.2018  |

    Og følgende dagens dato 25.10.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser              |
      | 1       | UTVIDET_BARNETRYGD            |                  |            |            | IKKE_OPPFYLT | Ja                   | AVSLAG_BOR_IKKE_FAST_MED_BARNET   |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 30.07.1983 |            | OPPFYLT      | Nei                  |                                   |

      | 2       | BOR_MED_SØKER                 |                  |            |            | IKKE_OPPFYLT | Ja                   | AVSLAG_IKKE_AVTALE_OM_DELT_BOSTED |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 28.07.2012 |            | OPPFYLT      | Nei                  |                                   |
      | 2       | GIFT_PARTNERSKAP              |                  | 28.07.2012 |            | OPPFYLT      | Nei                  |                                   |
      | 2       | UNDER_18_ÅR                   |                  | 28.07.2012 | 27.07.2030 | OPPFYLT      | Nei                  |                                   |

      | 3       | BOR_MED_SØKER                 |                  |            |            | IKKE_OPPFYLT | Ja                   | AVSLAG_IKKE_AVTALE_OM_DELT_BOSTED |
      | 3       | LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 24.06.2014 |            | OPPFYLT      | Nei                  |                                   |
      | 3       | GIFT_PARTNERSKAP              |                  | 24.06.2014 |            | OPPFYLT      | Nei                  |                                   |
      | 3       | UNDER_18_ÅR                   |                  | 24.06.2014 | 23.06.2032 | OPPFYLT      | Nei                  |                                   |

      | 4       | BOR_MED_SØKER                 |                  |            |            | IKKE_OPPFYLT | Ja                   | AVSLAG_IKKE_AVTALE_OM_DELT_BOSTED |
      | 4       | UNDER_18_ÅR                   |                  | 09.05.2018 | 08.05.2036 | OPPFYLT      | Nei                  |                                   |
      | 4       | GIFT_PARTNERSKAP              |                  | 09.05.2018 |            | OPPFYLT      | Nei                  |                                   |
      | 4       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 09.05.2018 |            | OPPFYLT      | Nei                  |                                   |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato | Til dato | Beløp | Ytelse type | Prosent | Sats |

    Når vedtaksperiodene genereres for behandling 1

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato | Til dato | Standardbegrunnelser                                                                                                                     | Eøsbegrunnelser | Fritekster |
      |          |          | AVSLAG_IKKE_AVTALE_OM_DELT_BOSTED, AVSLAG_IKKE_AVTALE_OM_DELT_BOSTED, AVSLAG_BOR_IKKE_FAST_MED_BARNET, AVSLAG_IKKE_AVTALE_OM_DELT_BOSTED |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode - til -
      | Begrunnelse                       | Type     | Gjelder søker | Barnas fødselsdatoer           | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp | Søkers rett til utvidet |
      | AVSLAG_IKKE_AVTALE_OM_DELT_BOSTED | STANDARD | Nei           | 28.07.12, 24.06.14 og 09.05.18 | 3           |                                      | NB      | 0     |                         |
      | AVSLAG_BOR_IKKE_FAST_MED_BARNET   | STANDARD | Ja            | 28.07.12, 24.06.14 og 09.05.18 | 0           |                                      | NB      | 0     |                         |

  Scenario: Skal flette inn barn i begrunnelse når vilkårene til barn er oppfylt, men det er avslag på søker
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | AVSLÅTT             | SØKNAD           | Nei                       | NASJONAL            |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 19.05.1997  |
      | 1            | 2       | BARN       | 07.08.2020  |

    Og følgende dagens dato 26.10.2023
    Og lag personresultater for begrunnelse for behandling 1

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser                              |
      | 1       | LOVLIG_OPPHOLD                              |                  |            |            | IKKE_OPPFYLT | Ja                   | AVSLAG_IKKE_OPPHOLDSTILLATELSE_MER_ENN_12_MÅNEDER |
      | 1       | BOSATT_I_RIKET                              |                  | 30.05.2023 |            | IKKE_OPPFYLT | Nei                  |                                                   |

      | 2       | GIFT_PARTNERSKAP                            |                  | 07.08.2020 |            | OPPFYLT      | Nei                  |                                                   |
      | 2       | UNDER_18_ÅR                                 |                  | 07.08.2020 | 06.08.2038 | OPPFYLT      | Nei                  |                                                   |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 30.05.2023 |            | OPPFYLT      | Nei                  |                                                   |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato | Til dato | Beløp | Ytelse type | Prosent | Sats |

    Når vedtaksperiodene genereres for behandling 1

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato | Til dato | Standardbegrunnelser                              | Eøsbegrunnelser | Fritekster |
      |          |          | AVSLAG_IKKE_OPPHOLDSTILLATELSE_MER_ENN_12_MÅNEDER |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode - til -
      | Begrunnelse                                       | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Beløp |
      | AVSLAG_IKKE_OPPHOLDSTILLATELSE_MER_ENN_12_MÅNEDER | STANDARD | Ja            | 07.08.20             | 0           | 0     | 