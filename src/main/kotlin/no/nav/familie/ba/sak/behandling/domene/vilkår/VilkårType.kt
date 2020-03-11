package no.nav.familie.ba.sak.behandling.domene.vilkår

import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType

enum class VilkårType(val beskrivelse: String,
                      val parterDetteGjelderFor: List<PersonType>,
                      val sakstyperDetteGjelderFor: List<Any>,
                      val lovreferanse: String) {

    UNDER_18_ÅR_OG_BOR_MED_SØKER(beskrivelse = "Er under 18 år og bor med søker",
                                 parterDetteGjelderFor = listOf<PersonType>(PersonType.BARN),
                                 sakstyperDetteGjelderFor = listOf<Any>("TESTSAKSTYPE"),
                                 lovreferanse = "§2"),
    BOSATT_I_RIKET(beskrivelse = "Bosatt i riket",
                   parterDetteGjelderFor = listOf<PersonType>(PersonType.BARN, PersonType.SØKER),
                   sakstyperDetteGjelderFor = listOf<Any>("TESTSAKSTYPE"),
                   lovreferanse = "§4"),
    STØNADSPERIODE(beskrivelse = "Barnetrygd gis fra og med kalendermåneden etter at retten til barnetrygd inntrer",
                   parterDetteGjelderFor = listOf(PersonType.BARN, PersonType.SØKER),
                   sakstyperDetteGjelderFor = listOf<Any>("TESTSAKSTYPE"),
                   lovreferanse = "§11");

    companion object {
        fun hentVilkårTyperForPart(personType: PersonType) = values()
                .filter { it.parterDetteGjelderFor.find { part -> part === personType } != null }
    }
}