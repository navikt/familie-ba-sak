package no.nav.familie.ba.sak.behandling.domene.vilkår

import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.nare.core.specifications.Spesifikasjon
import no.nav.nare.core.evaluations.Evaluering


enum class Vilkår(val parterDetteGjelderFor: List<PersonType>,
                  val sakstyperDetteGjelderFor: List<Any>,
                  val spesifikasjon: Spesifikasjon<Fakta>) {

    UNDER_18_ÅR_OG_BOR_MED_SØKER(
            parterDetteGjelderFor = listOf<PersonType>(PersonType.BARN),
            sakstyperDetteGjelderFor = listOf<Any>("TESTSAKSTYPE"),
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "§2 - Er under 18 år og bor med søker",
                    identifikator = "UNDER_18_ÅR_OG_BOR_MED_SØKER",
                    implementasjon = { barnUnder18ÅrOgBorMedSøker(this) })),
    BOSATT_I_RIKET(
            parterDetteGjelderFor = listOf<PersonType>(PersonType.SØKER, PersonType.BARN),
            sakstyperDetteGjelderFor = listOf<Any>("TESTSAKSTYPE"),
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "§4 - Bosatt i riket",
                    identifikator = "BOSATT_I_RIKET",
                    implementasjon = { bosattINorge(this) })),
    STØNADSPERIODE(
            parterDetteGjelderFor = listOf<PersonType>(PersonType.BARN, PersonType.SØKER),
            sakstyperDetteGjelderFor = listOf<Any>("TESTSAKSTYPE"),
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "§22 - Barnetrygd gis fra og med kalendermåneden etter at retten til barnetrygd inntrer",
                    identifikator = "STØNADSPERIODE",
                    implementasjon = { Evaluering.ja("Dette er grunnen") }));

    companion object {
        fun hentVilkårForPart(personType: PersonType) = values()
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