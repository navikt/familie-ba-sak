# language: no
# encoding: UTF-8

Egenskap: Brevbegrunnelser ved opphør for EØS.

  Scenario: Skal ikke dra inn barn som har blitt avslått tidligere i behandlingen i opphørsbegrunnelse i EØS-sak
    Gitt følgende fagsaker for begrunnelse
      | FagsakId | Fagsaktype |
      | 1        | NORMAL     |

    Gitt følgende behandling
      | BehandlingId | FagsakId | ForrigeBehandlingId | Behandlingsresultat | Behandlingsårsak | Skal behandles automatisk | Behandlingskategori |
      | 1            | 1        |                     | DELVIS_INNVILGET    | SØKNAD           | Nei                       | EØS                 |

    Og følgende persongrunnlag for begrunnelse
      | BehandlingId | AktørId | Persontype | Fødselsdato | Dødsfalldato |
      | 1            | 1       | SØKER      | 06.06.1994  |              |
      | 1            | 2       | BARN       | 16.01.2020  |              |
      | 1            | 3       | BARN       | 25.06.2020  |              |

    Og følgende dagens dato 23.02.2022
    Og lag personresultater for begrunnelse for behandling 1

    Og med personer fremstilt krav for i behandling
      | BehandlingId | AktørId |
      | 1            | 2       |
      | 1            | 3       |

    Og legg til nye vilkårresultater for begrunnelse for behandling 1
      | AktørId | Vilkår           | Utdypende vilkår             | Fra dato   | Til dato   | Resultat     | Er eksplisitt avslag | Standardbegrunnelser            | Vurderes etter   |
      | 1       | BOSATT_I_RIKET   | OMFATTET_AV_NORSK_LOVGIVNING | 01.09.2019 | 31.07.2020 | OPPFYLT      | Nei                  |                                 | EØS_FORORDNINGEN |
      | 1       | LOVLIG_OPPHOLD   |                              | 01.09.2019 | 31.07.2020 | OPPFYLT      | Nei                  |                                 | EØS_FORORDNINGEN |

      | 2       | UNDER_18_ÅR      |                              | 16.01.2020 | 15.01.2038 | OPPFYLT      | Nei                  |                                 |                  |
      | 2       | BOR_MED_SØKER    |                              | 16.01.2020 |            | IKKE_OPPFYLT | Ja                   | AVSLAG_EØS_IKKE_ANSVAR_FOR_BARN | EØS_FORORDNINGEN |
      | 2       | LOVLIG_OPPHOLD   |                              | 16.01.2020 |            | OPPFYLT      | Nei                  |                                 | EØS_FORORDNINGEN |
      | 2       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 16.01.2020 |            | OPPFYLT      | Nei                  |                                 | EØS_FORORDNINGEN |
      | 2       | GIFT_PARTNERSKAP |                              | 16.01.2020 |            | OPPFYLT      | Nei                  |                                 |                  |

      | 3       | BOR_MED_SØKER    | BARN_BOR_I_EØS_MED_SØKER     | 25.06.2020 |            | OPPFYLT      | Nei                  |                                 | EØS_FORORDNINGEN |
      | 3       | UNDER_18_ÅR      |                              | 25.06.2020 | 24.06.2038 | OPPFYLT      | Nei                  |                                 |                  |
      | 3       | LOVLIG_OPPHOLD   |                              | 25.06.2020 |            | OPPFYLT      | Nei                  |                                 | EØS_FORORDNINGEN |
      | 3       | GIFT_PARTNERSKAP |                              | 25.06.2020 |            | OPPFYLT      | Nei                  |                                 |                  |
      | 3       | BOSATT_I_RIKET   | BARN_BOR_I_EØS               | 25.06.2020 |            | OPPFYLT      | Nei                  |                                 | EØS_FORORDNINGEN |

    Og med andeler tilkjent ytelse for begrunnelse
      | AktørId | BehandlingId | Fra dato   | Til dato   | Beløp | Ytelse type        | Prosent | Sats |
      | 3       | 1            | 01.07.2020 | 31.07.2020 | 0     | ORDINÆR_BARNETRYGD | 100     | 1054 |

    Og med kompetanser for begrunnelse
      | AktørId | Fra dato   | Til dato   | Resultat              | BehandlingId | Søkers aktivitet | Annen forelders aktivitet | Søkers aktivitetsland | Annen forelders aktivitetsland | Barnets bostedsland |
      | 3       | 01.07.2020 | 31.07.2020 | NORGE_ER_SEKUNDÆRLAND | 1            | ARBEIDER         | I_ARBEID                  | NO                    | PL                             | PL                  |

    Når vedtaksperiodene genereres for behandling 1

    Og når disse begrunnelsene er valgt for behandling 1
      | Fra dato   | Til dato | Standardbegrunnelser | Eøsbegrunnelser                 | Fritekster |
      | 01.02.2020 |          |                      | AVSLAG_EØS_IKKE_ANSVAR_FOR_BARN |            |
      | 01.08.2020 |          |                      | OPPHØR_EØS_STANDARD             |            |

    Så forvent følgende brevbegrunnelser for behandling 1 i periode 01.08.2020 til -
      | Begrunnelse         | Type | Barnas fødselsdatoer | Antall barn | Gjelder søker | Målform |
      | OPPHØR_EØS_STANDARD | EØS  | 25.06.20             | 1           | Ja            | NB      |