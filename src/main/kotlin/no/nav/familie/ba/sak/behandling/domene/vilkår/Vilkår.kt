package no.nav.familie.ba.sak.behandling.domene.vilkår

import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.nare.core.specifications.Spesifikasjon
import no.nav.nare.core.evaluations.Evaluering

enum class Vilkår(val parterDetteGjelderFor: List<PersonType>,
                  val sakstyperDetteGjelderFor: List<Any>,
                  val spesifikasjon: Spesifikasjon<Fakta>) {

    UNDER_18_ÅR_OG_BOR_MED_SØKER(parterDetteGjelderFor = listOf<PersonType>(PersonType.BARN),
                                 sakstyperDetteGjelderFor = listOf<Any>("TESTSAKSTYPE"),
                                 spesifikasjon = Spesifikasjon(
                                         beskrivelse = "Er under 18 år og bor med søker",
                                         identifikator = "§2",
                                         implementasjon = {
                                             when {
                                                 this.barn.isNotEmpty() -> Evaluering.ja(
                                                         "Barn er under 18 år og bor med søker"
                                                 )
                                                 else -> Evaluering.nei("Barn er ikke under 18 år eller bor ikke med søker")
                                             }
                                         }
                                 )),
    BOSATT_I_RIKET(parterDetteGjelderFor = listOf<PersonType>(PersonType.BARN, PersonType.SØKER),
                   sakstyperDetteGjelderFor = listOf<Any>("TESTSAKSTYPE"),
                   spesifikasjon = Spesifikasjon(
                           beskrivelse = "§4 - Bosatt i riket",
                           identifikator = BOSATT_I_RIKET.name,
                           implementasjon = {
                               when {
                                   this.barn.isNotEmpty() -> Evaluering.ja(
                                           "Ja, dette er grunnen"
                                   )
                                   else -> Evaluering.nei("Nei, dette er grunnen")
                               }
                           }
                   )),
    STØNADSPERIODE(parterDetteGjelderFor = listOf<PersonType>(PersonType.BARN, PersonType.SØKER),
                   sakstyperDetteGjelderFor = listOf<Any>("TESTSAKSTYPE"),
                   spesifikasjon = Spesifikasjon(
                           beskrivelse = "Barnetrygd gis fra og med kalendermåneden etter at retten til barnetrygd inntrer",
                           identifikator = "§22",
                           implementasjon = {
                               when {
                                   this.barn.isNotEmpty() -> Evaluering.ja(
                                           "Ja, dette er grunnen"
                                   )
                                   else -> Evaluering.nei("Nei, dette er grunnen")
                               }
                           }
                   )),
    BARN_HAR_RETT_TIL(parterDetteGjelderFor = listOf<PersonType>(PersonType.BARN),
                      sakstyperDetteGjelderFor = listOf<Any>("TESTSAKSTYPE"),
                      spesifikasjon = (UNDER_18_ÅR_OG_BOR_MED_SØKER.spesifikasjon
                              og BOSATT_I_RIKET.spesifikasjon
                              og STØNADSPERIODE.spesifikasjon
                                      ));

    companion object {
        fun hentVilkårTyperForPart(personType: PersonType) = values()
                .filter { personType in it.parterDetteGjelderFor }.toSet()

        fun hentVilkårTyperForSakstype(sakstype: Any) = values()
                .filter { sakstype in it.sakstyperDetteGjelderFor }.toSet()

        fun hentVilkårFor(personType: PersonType, sakstype: Any): Set<Vilkår> {
            return values().filter {
                personType in it.parterDetteGjelderFor
                && sakstype in it.sakstyperDetteGjelderFor
            }.toSet()
        }
    }
}