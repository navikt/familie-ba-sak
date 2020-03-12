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
                                         beskrivelse = "§2 - Er under 18 år og bor med søker",
                                         identifikator = "UNDER_18_ÅR_OG_BOR_MED_SØKER",
                                         implementasjon = {
                                             when {
                                                 this.barn.isNotEmpty() -> Evaluering.ja(
                                                         "Barn er under 18 år og bor med søker"
                                                 )
                                                 else -> Evaluering.nei("Barn er ikke under 18 år eller bor ikke med søker")
                                             }
                                         }
                                 )),
    BOSATT_I_RIKET_SØKER(parterDetteGjelderFor = listOf<PersonType>(PersonType.SØKER),
                         sakstyperDetteGjelderFor = listOf<Any>("TESTSAKSTYPE"),
                         spesifikasjon = Spesifikasjon(
                                 beskrivelse = "§4 - Bosatt i riket",
                                 identifikator = "BOSATT_I_RIKET_SØKER",
                                 implementasjon = {
                                     sjekkOmBosattINorge(this.personopplysningGrunnlag.personer
                                                                 .filter { person -> person.type == PersonType.SØKER }
                                                                 .first())
                                 })),
    BOSATT_I_RIKET_BARN(parterDetteGjelderFor = listOf<PersonType>(PersonType.BARN),
                        sakstyperDetteGjelderFor = listOf<Any>("TESTSAKSTYPE"),
                        spesifikasjon = Spesifikasjon(
                                beskrivelse = "§4 - Bosatt i riket",
                                identifikator = "BOSATT_I_RIKET_BARN",
                                implementasjon = { sjekkOmBosattINorge(this.barn[0]) }
                                //Hvordan håndtere kjøring av samme regel på flere barn?
                                //Forslag: Faktagrunnlag inneholder en "hoved"-Person for vurderingen i tillegg til andre fakta.
                                //Denne kan settes f.eks. når vilkår for type hentes?
                                //Da vil man også slippe en egen regel for søker og barn på f.eks. bosatt i riket
                        )),
    STØNADSPERIODE(parterDetteGjelderFor = listOf<PersonType>(PersonType.BARN, PersonType.SØKER),
                   sakstyperDetteGjelderFor = listOf<Any>("TESTSAKSTYPE"),
                   spesifikasjon = Spesifikasjon(
                           beskrivelse = "§22 - Barnetrygd gis fra og med kalendermåneden etter at retten til barnetrygd inntrer",
                           identifikator = "STØNADSPERIODE",
                           implementasjon = {
                               when {
                                   this.barn.isNotEmpty() -> Evaluering.ja(
                                           "Ja, dette er grunnen"
                                   )
                                   else -> Evaluering.nei("Nei, dette er grunnen")
                               }
                           }
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