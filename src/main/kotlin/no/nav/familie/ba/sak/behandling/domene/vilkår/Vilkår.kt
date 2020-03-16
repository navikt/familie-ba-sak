package no.nav.familie.ba.sak.behandling.domene.vilkår

import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.nare.core.specifications.Spesifikasjon
import no.nav.nare.core.evaluations.Evaluering
import java.time.LocalDate


enum class Vilkår(val parterDetteGjelderFor: List<PersonType>,
                  val sakstyperDetteGjelderFor: List<SakType>,
                  val spesifikasjon: Spesifikasjon<Fakta>,
                  val gyldigVilkårsperiode: GyldigVilkårsperiode) {

    UNDER_18_ÅR_OG_BOR_MED_SØKER(
            parterDetteGjelderFor = listOf<PersonType>(PersonType.BARN),
            sakstyperDetteGjelderFor = listOf<SakType>(SakType.VILKÅRGJELDERFOR),
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "§2 - Er under 18 år og bor med søker",
                    identifikator = "UNDER_18_ÅR_OG_BOR_MED_SØKER",
                    implementasjon = { barnUnder18ÅrOgBorMedSøker(this) }),
            gyldigVilkårsperiode = GyldigVilkårsperiode()),
    BOSATT_I_RIKET(
            parterDetteGjelderFor = listOf<PersonType>(PersonType.SØKER, PersonType.BARN),
            sakstyperDetteGjelderFor = listOf<SakType>(SakType.VILKÅRGJELDERFOR),
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "§4 - Bosatt i riket",
                    identifikator = "BOSATT_I_RIKET",
                    implementasjon = { bosattINorge(this) }),
            gyldigVilkårsperiode = GyldigVilkårsperiode()),
    STØNADSPERIODE(
            parterDetteGjelderFor = listOf<PersonType>(PersonType.BARN, PersonType.SØKER),
            sakstyperDetteGjelderFor = listOf<SakType>(SakType.VILKÅRGJELDERFOR),
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "§22 - Barnetrygd gis fra og med kalendermåneden etter at retten til barnetrygd inntrer",
                    identifikator = "STØNADSPERIODE",
                    implementasjon = { Evaluering.ja("Dette er grunnen") }),
            gyldigVilkårsperiode = GyldigVilkårsperiode());

    companion object {
        fun hentVilkårForPart(personType: PersonType) = values()
                .filter { personType in it.parterDetteGjelderFor }.toSet()

        fun hentVilkårForPart(personType: PersonType, vurderingsDato: LocalDate) = values()
                .filter { personType in it.parterDetteGjelderFor && it.gyldigVilkårsperiode.gyldigFor(vurderingsDato) }.
                toSet()

        fun hentVilkårForSakstype(sakstype: SakType) = values()
                .filter { sakstype in it.sakstyperDetteGjelderFor }.toSet()

        fun hentVilkårFor(personType: PersonType, sakstype: SakType): Set<Vilkår> {
            return values().filter {
                personType in it.parterDetteGjelderFor
                && sakstype in it.sakstyperDetteGjelderFor
            }.toSet()
        }
    }
}

data class GyldigVilkårsperiode (
        val gyldigFom: LocalDate = LocalDate.MIN,
        val gyldigTom: LocalDate = LocalDate.MAX
) {
    fun gyldigFor(dato: LocalDate): Boolean {
        return !(dato.isBefore(gyldigFom) || dato.isAfter(gyldigTom))
    }
}

enum class SakType {
    VILKÅRGJELDERFOR, VILKÅRGJELDERIKKEFOR
}