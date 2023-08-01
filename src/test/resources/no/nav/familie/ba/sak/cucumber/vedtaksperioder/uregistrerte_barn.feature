# language: no
# encoding: UTF-8

Egenskap: Vedtaksperioder med mor og to barn

  Bakgrunn:
    Gitt følgende vedtak
      | BehandlingId |
      | 1            |

    Og følgende persongrunnlag
      | BehandlingId | AktørId | Persontype | Fødselsdato |
      | 1            | 1234    | SØKER      | 11.01.1970  |

  Scenario: Skal lage avslagsperiode uten datoer når vi har uregistrerte barn

    Og lag personresultater for behandling 1
    Og legg til nye vilkårresultater for behandling 1
      | AktørId | Vilkår                         | Fra dato   | Til dato | Resultat |
      | 1234    | BOSATT_I_RIKET, LOVLIG_OPPHOLD | 11.01.1970 |          | Oppfylt  |

    Og med uregistrerte barn

    Når vedtaksperioder med begrunnelser genereres for behandling 1

    Så forvent følgende vedtaksperioder med begrunnelser
      | Fra dato | Til dato | Vedtaksperiodetype | Kommentar | Begrunnelser            |
      |          |          | Avslag             |           | AVSLAG_UREGISTRERT_BARN |




