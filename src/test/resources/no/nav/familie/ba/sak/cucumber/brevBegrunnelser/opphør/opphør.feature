# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser ved opphør

  Bakgrunn:
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat  | Behandlingsårsak | Skal behandles automatisk |
      | 1            | 1        |                     | INNVILGET_OG_OPPHØRT | SØKNAD           | Nei                       |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 10.07.1988  |
      | 1            | 2       | BARN       | 17.04.2017  |
      | 1            | 3       | BARN       | 11.07.2013  |
      | 1            | 4       | BARN       | 16.04.2017  |

  Scenario: Skal kun flette inn barna som hadde andeler forrige periode når vi opphører på søker.
    Og dagens dato er 13.10.2023
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD                                               |                  | 10.07.1988 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                                               |                  | 01.12.2022 | 01.02.2023 | OPPFYLT  | Nei                  |

      | 3       | GIFT_PARTNERSKAP,BOR_MED_SØKER,LOVLIG_OPPHOLD                |                  | 11.07.2013 |            | OPPFYLT  | Nei                  |
      | 3       | UNDER_18_ÅR                                                  |                  | 11.07.2013 | 10.07.2031 | OPPFYLT  | Nei                  |
      | 3       | BOSATT_I_RIKET                                               |                  | 01.12.2022 | 01.01.2023 | OPPFYLT  | Nei                  |

      | 2       | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 17.04.2017 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                                  |                  | 17.04.2017 | 16.04.2035 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3       | 1            | 01.01.2023 | 31.01.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.01.2023 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |

    Når vedtaksperiodene genereres for behandling 1

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato | Standardbegrunnelser | Eøsbegrunnelser | Fritekster |
      | 01.03.2023 |          | OPPHØR_UTVANDRET     |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.03.2023 til -
      | Begrunnelse      | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp |
      | OPPHØR_UTVANDRET | STANDARD | Ja            | 17.04.17             | 0           | februar 2023                         | NB      | 0     |

  Scenario: Skal flette inn barn som har oppfylte vilkår og barn som hadde andeler i forrige periode når vi opphører for søker
    Og dagens dato er 13.10.2023
    Og lag personresultater for behandling 1

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag |
      | 1       | LOVLIG_OPPHOLD                                               |                  | 10.07.1988 |            | OPPFYLT  | Nei                  |
      | 1       | BOSATT_I_RIKET                                               |                  | 01.12.2022 | 01.02.2023 | OPPFYLT  | Nei                  |

      | 2       | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOSATT_I_RIKET,BOR_MED_SØKER |                  | 17.04.2017 |            | OPPFYLT  | Nei                  |
      | 2       | UNDER_18_ÅR                                                  |                  | 17.04.2017 | 16.04.2035 | OPPFYLT  | Nei                  |

      | 4       | GIFT_PARTNERSKAP,LOVLIG_OPPHOLD,BOR_MED_SØKER                |                  | 17.04.2017 |            | OPPFYLT  | Nei                  |
      | 4       | BOSATT_I_RIKET                                               |                  | 17.04.2017 | 01.02.2023 | OPPFYLT  | Nei                  |
      | 4       | UNDER_18_ÅR                                                  |                  | 17.04.2017 | 16.04.2035 | OPPFYLT  | Nei                  |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.01.2023 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 4       | 1            | 01.01.2023 | 28.02.2023 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |

    Når vedtaksperiodene genereres for behandling 1

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato | Standardbegrunnelser | Eøsbegrunnelser | Fritekster |
      | 01.03.2023 |          | OPPHØR_UTVANDRET     |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.03.2023 til -
      | Begrunnelse      | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp |
      | OPPHØR_UTVANDRET | STANDARD | Ja            | 16.04.17 og 17.04.17 | 1           | februar 2023                         | NB      | 0     |

  Scenario: Skal flette inn barnas fødselsdatoer men ikke "du og barna" når vi begrunner opphør på søkers vilkår
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | ENDRET_UTBETALING   | SATSENDRING      | Ja                        | NASJONAL            |
      | 2            | 1        | 1                   | OPPHØRT             | NYE_OPPLYSNINGER | Nei                       | NASJONAL            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 15.01.1988  |
      | 1            | 2       | BARN       | 06.03.2012  |
      | 1            | 3       | BARN       | 21.12.2016  |
      | 2            | 1       | SØKER      | 15.01.1988  |
      | 2            | 2       | BARN       | 06.03.2012  |
      | 2            | 3       | BARN       | 21.12.2016  |
    Og dagens dato er 30.10.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser |
      | 1       | BOSATT_I_RIKET,LOVLIG_OPPHOLD               |                  | 01.01.2022 |            | OPPFYLT  | Nei                  |                      |

      | 2       | GIFT_PARTNERSKAP                            |                  | 06.03.2012 |            | OPPFYLT  | Nei                  |                      |
      | 2       | UNDER_18_ÅR                                 |                  | 06.03.2012 | 05.03.2030 | OPPFYLT  | Nei                  |                      |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.01.2022 |            | OPPFYLT  | Nei                  |                      |

      | 3       | GIFT_PARTNERSKAP                            |                  | 21.12.2016 |            | OPPFYLT  | Nei                  |                      |
      | 3       | UNDER_18_ÅR                                 |                  | 21.12.2016 | 20.12.2034 | OPPFYLT  | Nei                  |                      |
      | 3       | BOR_MED_SØKER,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 25.02.2022 |            | OPPFYLT  | Nei                  |                      |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser |
      | 1       | BOSATT_I_RIKET                              |                  | 01.01.2022 | 16.07.2023 | OPPFYLT      | Nei                  |                      |
      | 1       | LOVLIG_OPPHOLD                              |                  | 01.01.2022 |            | OPPFYLT      | Nei                  |                      |
      | 1       | BOSATT_I_RIKET                              |                  | 17.07.2023 |            | IKKE_OPPFYLT | Nei                  |                      |

      | 2       | GIFT_PARTNERSKAP                            |                  | 06.03.2012 |            | OPPFYLT      | Nei                  |                      |
      | 2       | UNDER_18_ÅR                                 |                  | 06.03.2012 | 05.03.2030 | OPPFYLT      | Nei                  |                      |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.01.2022 |            | OPPFYLT      | Nei                  |                      |

      | 3       | GIFT_PARTNERSKAP                            |                  | 21.12.2016 |            | OPPFYLT      | Nei                  |                      |
      | 3       | UNDER_18_ÅR                                 |                  | 21.12.2016 | 20.12.2034 | OPPFYLT      | Nei                  |                      |
      | 3       | BOR_MED_SØKER,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                  | 25.02.2022 |            | OPPFYLT      | Nei                  |                      |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.02.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 1            | 01.07.2023 | 28.02.2030 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 1            | 01.03.2022 | 30.11.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 1            | 01.12.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 1            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 1            | 01.07.2023 | 30.11.2034 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

      | 2       | 2            | 01.02.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.07.2023 | 31.07.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |
      | 3       | 2            | 01.03.2022 | 30.11.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |
      | 3       | 2            | 01.12.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 3       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 3       | 2            | 01.07.2023 | 31.07.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser | Ugyldige begrunnelser |
      | 01.08.2023 |          | OPPHØR             |                                | OPPHØR_UTVANDRET     |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser | Eøsbegrunnelser | Fritekster |
      | 01.08.2023 |          | OPPHØR_UTVANDRET     |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.08.2023 til -
      | Begrunnelse      | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp |
      | OPPHØR_UTVANDRET | STANDARD | Ja            | 06.03.12 og 21.12.16 | 0           | juli 2023                            | NB      | 0     |

  Scenario: Skal ta med barna som har mistet utbetaling fra forrige behandling på opphørsbegrunnelse som gjelder søker
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | OPPHØRT             | NYE_OPPLYSNINGER | Nei                       | NASJONAL            |
      | 2            | 1        | 1                   | INNVILGET_OG_ENDRET | SØKNAD           | Nei                       | EØS                 |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 22.12.1977  |
      | 1            | 2       | BARN       | 04.05.2006  |
      | 2            | 1       | SØKER      | 22.12.1977  |
      | 2            | 2       | BARN       | 04.05.2006  |

    Og dagens dato er 09.11.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                        | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser |
      | 1       | LOVLIG_OPPHOLD                |                  | 01.04.2022 |            | OPPFYLT      | Nei                  |                      |
      | 1       | BOSATT_I_RIKET                |                  | 01.04.2022 | 21.05.2022 | OPPFYLT      | Nei                  |                      |
      | 1       | BOSATT_I_RIKET                |                  | 22.05.2022 |            | IKKE_OPPFYLT | Nei                  |                      |

      | 2       | UNDER_18_ÅR                   |                  | 04.05.2006 | 03.05.2024 | OPPFYLT      | Nei                  |                      |
      | 2       | GIFT_PARTNERSKAP              |                  | 04.05.2006 |            | OPPFYLT      | Nei                  |                      |
      | 2       | BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 01.04.2022 |            | OPPFYLT      | Nei                  |                      |
      | 2       | BOR_MED_SØKER                 | DELT_BOSTED      | 01.04.2022 |            | OPPFYLT      | Nei                  |                      |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser |
      | 1       | LOVLIG_OPPHOLD   |                              | 04.08.2022 | 15.09.2023 | OPPFYLT  | Nei                  |                      |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 04.08.2022 | 15.09.2023 | OPPFYLT  | Nei                  |                      |

      | 2       | GIFT_PARTNERSKAP |                              | 04.05.2006 |            | OPPFYLT  | Nei                  |                      |
      | 2       | UNDER_18_ÅR      |                              | 04.05.2006 | 03.05.2024 | OPPFYLT  | Nei                  |                      |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 04.08.2022 | 15.09.2023 | OPPFYLT  | Nei                  |                      |
      | 2       | LOVLIG_OPPHOLD   |                              | 04.08.2022 | 15.09.2023 | OPPFYLT  | Nei                  |                      |
      | 2       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 04.08.2022 | 15.09.2023 | OPPFYLT  | Nei                  |                      |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 2       | 1            | 01.05.2022 | 31.05.2022 | 527   | ORDINÆR_BARNETRYGD | 50      | 1054 |

      | 2       | 2            | 01.09.2022 | 28.02.2023 | 1054  | ORDINÆR_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.03.2023 | 30.06.2023 | 1083  | ORDINÆR_BARNETRYGD | 100     | 1083 |
      | 2       | 2            | 01.07.2023 | 30.09.2023 | 1310  | ORDINÆR_BARNETRYGD | 100     | 1310 |

    Og med kompetanser
      | AktørId | Fra dato   | Til dato   | Resultat            | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 2       | 01.09.2022 | 30.09.2023 | NORGE_ER_PRIMÆRLAND | 2            | ARBEIDER         | I_ARBEID                  | NO                    | NO                             | SE                  |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato   | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser       | Ugyldige begrunnelser |
      | 01.05.2022 | 31.08.2022 | OPPHØR             |                                | OPPHØR_IKKE_BOSATT_I_NORGE |                       |


    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato   | Standardbegrunnelser       | Eøsbegrunnelser | Fritekster |
      | 01.05.2022 | 31.08.2022 | OPPHØR_IKKE_BOSATT_I_NORGE |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.05.2022 til 31.08.2022
      | Begrunnelse                | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp |
      | OPPHØR_IKKE_BOSATT_I_NORGE | STANDARD | Ja            | 04.05.06             | 1           | april 2022                           | NB      | 0     |

  Scenario: Skal flette inn riktige barn i opphørstest
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | OPPHØRT             | NYE_OPPLYSNINGER | Nei                       | NASJONAL            |
      | 2            | 1        | 1                   | AVSLÅTT             | SØKNAD           | Nei                       | NASJONAL            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1       | SØKER      | 30.12.2001  |
      | 1            | 2       | BARN       | 25.12.2018  |
      | 2            | 1       | SØKER      | 30.12.2001  |
      | 2            | 2       | BARN       | 25.12.2018  |

    Og dagens dato er 01.12.2023
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår         | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET                              | VURDERING_ANNET_GRUNNLAG | 01.11.2021 | 31.01.2022 | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD                          |                          | 01.11.2021 |            | OPPFYLT  | Nei                  |                      |                  |
      | 1       | LOVLIG_OPPHOLD                              |                          | 01.11.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 2       | GIFT_PARTNERSKAP                            |                          | 25.12.2018 |            | OPPFYLT  | Nei                  |                      |                  |
      | 2       | UNDER_18_ÅR                                 |                          | 25.12.2018 | 24.12.2036 | OPPFYLT  | Nei                  |                      |                  |
      | 2       | BOSATT_I_RIKET,BOR_MED_SØKER,LOVLIG_OPPHOLD |                          | 01.11.2021 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                                      | Utdypende vilkår         | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser                   | Vurderes etter   |
      | 1       | BOSATT_I_RIKET                              |                          |            |            | IKKE_OPPFYLT | Ja                   | AVSLAG_IKKE_DOKUMENTERT_BOSATT_I_NORGE | NASJONALE_REGLER |
      | 1       | BOSATT_I_RIKET                              | VURDERING_ANNET_GRUNNLAG | 01.11.2021 | 31.01.2022 | OPPFYLT      | Nei                  |                                        | NASJONALE_REGLER |
      | 1       | LOVLIG_OPPHOLD                              |                          | 01.11.2021 |            | OPPFYLT      | Nei                  |                                        | NASJONALE_REGLER |
      | 1       | UTVIDET_BARNETRYGD                          |                          | 01.11.2021 |            | OPPFYLT      | Nei                  |                                        |                  |

      | 2       | UNDER_18_ÅR                                 |                          | 25.12.2018 | 24.12.2036 | OPPFYLT      | Nei                  |                                        |                  |
      | 2       | GIFT_PARTNERSKAP                            |                          | 25.12.2018 |            | OPPFYLT      | Nei                  |                                        |                  |
      | 2       | BOR_MED_SØKER,LOVLIG_OPPHOLD,BOSATT_I_RIKET |                          | 01.11.2021 |            | OPPFYLT      | Nei                  |                                        | NASJONALE_REGLER |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 1       | 1            | 01.12.2021 | 31.01.2022 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 2       | 1            | 01.12.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     | 1654 |
      | 2       | 1            | 01.01.2022 | 31.01.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |

      | 1       | 2            | 01.12.2021 | 31.01.2022 | 1054  | UTVIDET_BARNETRYGD | 100     | 1054 |
      | 2       | 2            | 01.12.2021 | 31.12.2021 | 1654  | ORDINÆR_BARNETRYGD | 100     | 1654 |
      | 2       | 2            | 01.01.2022 | 31.01.2022 | 1676  | ORDINÆR_BARNETRYGD | 100     | 1676 |

    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                   | Ugyldige begrunnelser |
      |            |          | AVSLAG             |                                | AVSLAG_IKKE_DOKUMENTERT_BOSATT_I_NORGE |                       |
      | 01.02.2022 |          | OPPHØR             |                                |                                        |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser                   | Eøsbegrunnelser | Fritekster |
      |            |          | AVSLAG_IKKE_DOKUMENTERT_BOSATT_I_NORGE |                 |            |
      | 01.02.2022 |          | OPPHØR_VURDERING_IKKE_BOSATT_I_NORGE   |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.02.2022 til -
      | Begrunnelse                          | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp |
      | OPPHØR_VURDERING_IKKE_BOSATT_I_NORGE | STANDARD | Ja            | 25.12.18             | 0           | januar 2022                          | NB      | 0     |

  Scenario: Skal ikke flette inn barn med eksplisitt avslag i opphørstekst
    Gitt følgende fagsaker
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandlinger
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | FORTSATT_INNVILGET  | SATSENDRING      | Ja                        | NASJONAL            |
      | 2            | 1        | 1                   | AVSLÅTT_OG_OPPHØRT  | SØKNAD           | Nei                       | NASJONAL            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 09.05.1995  |              |
      | 1            | 3       | BARN       | 08.12.2023  |              |
      | 2            | 1       | SØKER      | 09.05.1995  |              |
      | 2            | 2       | BARN       | 06.06.2017  |              |
      | 2            | 3       | BARN       | 08.12.2023  |              |

    Og dagens dato er 12.02.2024
    Og lag personresultater for behandling 1
    Og lag personresultater for behandling 2


    Og med personer fremstilt krav for
      | BehandlingId | AktørId |
      | 2            | 2       |
      | 2            | 3       |

    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                                      | Utdypende vilkår | Fra dato   | Til dato   | Resultat | Er eksplisitt avslag | Standardbegrunnelser | Vurderes etter   |
      | 1       | BOSATT_I_RIKET                              |                  | 29.03.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |
      | 1       | LOVLIG_OPPHOLD                              |                  | 08.12.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR                                 |                  | 08.12.2023 | 07.12.2041 | OPPFYLT  | Nei                  |                      |                  |
      | 3       | GIFT_PARTNERSKAP                            |                  | 08.12.2023 |            | OPPFYLT  | Nei                  |                      |                  |
      | 3       | BOR_MED_SØKER,BOSATT_I_RIKET,LOVLIG_OPPHOLD |                  | 08.12.2023 |            | OPPFYLT  | Nei                  |                      | NASJONALE_REGLER |

    Og legg til nye vilkårresultater for behandling 2
      | AktørId | Vilkår                       | Utdypende vilkår | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser                              | Vurderes etter   |
      | 1       | BOSATT_I_RIKET               |                  | 29.03.2023 |            | OPPFYLT      | Nei                  |                                                   | NASJONALE_REGLER |
      | 1       | LOVLIG_OPPHOLD               |                  | 01.12.2023 |            | IKKE_OPPFYLT | Nei                  |                                                   | NASJONALE_REGLER |

      | 2       | LOVLIG_OPPHOLD               |                  |            |            | IKKE_OPPFYLT | Ja                   | AVSLAG_IKKE_OPPHOLDSTILLATELSE_MER_ENN_12_MÅNEDER | NASJONALE_REGLER |
      | 2       | GIFT_PARTNERSKAP             |                  | 06.06.2017 |            | OPPFYLT      | Nei                  |                                                   |                  |
      | 2       | UNDER_18_ÅR                  |                  | 06.06.2017 | 05.06.2035 | OPPFYLT      | Nei                  |                                                   |                  |
      | 2       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 29.03.2023 |            | OPPFYLT      | Nei                  |                                                   | NASJONALE_REGLER |

      | 3       | UNDER_18_ÅR                  |                  | 08.12.2023 | 07.12.2041 | OPPFYLT      | Nei                  |                                                   |                  |
      | 3       | BOR_MED_SØKER,BOSATT_I_RIKET |                  | 08.12.2023 |            | OPPFYLT      | Nei                  |                                                   | NASJONALE_REGLER |
      | 3       | GIFT_PARTNERSKAP             |                  | 08.12.2023 |            | OPPFYLT      | Nei                  |                                                   |                  |
      | 3       | LOVLIG_OPPHOLD               |                  | 08.12.2023 |            | IKKE_OPPFYLT | Nei                  |                                                   | NASJONALE_REGLER |

    Og med andeler tilkjent ytelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3       | 1            | 01.01.2024 | 30.11.2029 | 1766  | ORDINÆR_BARNETRYGD | 100     | 1766 |
      | 3       | 1            | 01.12.2029 | 30.11.2041 | 1510  | ORDINÆR_BARNETRYGD | 100     | 1510 |


    Når vedtaksperiodene genereres for behandling 2

    Så forvent at følgende begrunnelser er gyldige
      | Fra dato   | Til dato | VedtaksperiodeType | Regelverk Gyldige begrunnelser | Gyldige begrunnelser                              | Ugyldige begrunnelser |
      |            |          | AVSLAG             |                                | AVSLAG_IKKE_OPPHOLDSTILLATELSE_MER_ENN_12_MÅNEDER |                       |
      | 01.01.2024 |          | OPPHØR             |                                | OPPHØR_IKKE_OPPHOLDSTILLATELSE                    |                       |

    Og når disse begrunnelsene er valgt for behandling 2
      | Fra dato   | Til dato | Standardbegrunnelser                              | Eøsbegrunnelser | Fritekster |
      |            |          | AVSLAG_IKKE_OPPHOLDSTILLATELSE_MER_ENN_12_MÅNEDER |                 |            |
      | 01.01.2024 |          | OPPHØR_IKKE_OPPHOLDSTILLATELSE                    |                 |            |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode - til -
      | Begrunnelse                                       | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp |
      | AVSLAG_IKKE_OPPHOLDSTILLATELSE_MER_ENN_12_MÅNEDER | STANDARD | Nei           | 06.06.17             | 1           |                                      | NB      | 0     |

    Så forvent følgende brevbegrunnelser for behandling 2 i periode 01.01.2024 til -
      | Begrunnelse                    | Type     | Gjelder søker | Barnas fødselsdatoer | Antall barn | Måned og år begrunnelsen gjelder for | Målform | Beløp |
      | OPPHØR_IKKE_OPPHOLDSTILLATELSE | STANDARD | Ja            | 08.12.23             | 1           | desember 2023                        | NB      | 0     |

