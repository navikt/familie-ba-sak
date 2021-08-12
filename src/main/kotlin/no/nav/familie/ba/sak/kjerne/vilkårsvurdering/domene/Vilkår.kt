package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering.VurderBarnErBosattMedSøker
import no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering.VurderBarnErUgift
import no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering.VurderPersonErBosattIRiket
import no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering.VurderBarnErUnder18
import no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering.VurderPersonHarLovligOpphold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType.BARN
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType.SØKER
import java.time.LocalDate


enum class Vilkår(val parterDetteGjelderFor: List<PersonType>,
                  val beskrivelse: String) {

    UNDER_18_ÅR(
            parterDetteGjelderFor = listOf(BARN),
            beskrivelse = "Er under 18 år"),
    BOR_MED_SØKER(
            parterDetteGjelderFor = listOf(BARN),
            beskrivelse = "Bor med søker"),
    GIFT_PARTNERSKAP(
            parterDetteGjelderFor = listOf(BARN),
            beskrivelse = "Gift/partnerskap"),
    BOSATT_I_RIKET(
            parterDetteGjelderFor = listOf(SØKER, BARN),
            beskrivelse = "Bosatt i riket"),
    LOVLIG_OPPHOLD(
            parterDetteGjelderFor = listOf(SØKER, BARN),
            beskrivelse = "Lovlig opphold");

    override fun toString(): String {
        return this.name
    }

    companion object {

        fun hentVilkårFor(personType: PersonType): Set<Vilkår> {
            return values().filter {
                personType in it.parterDetteGjelderFor
            }.toSet()
        }

        fun hentFødselshendelseVilkårsreglerRekkefølge(): List<Vilkår> {
            return listOf(
                    UNDER_18_ÅR,
                    BOR_MED_SØKER,
                    GIFT_PARTNERSKAP,
                    BOSATT_I_RIKET,
                    LOVLIG_OPPHOLD,
            )
        }
    }

    fun vurderVilkår(person: Person, vurderFra: LocalDate = LocalDate.now()): AutomatiskVurdering {
        val vilkårsregel = when (this) {
            UNDER_18_ÅR -> VurderBarnErUnder18(
                    alder = person.hentAlder()
            )
            BOR_MED_SØKER -> VurderBarnErBosattMedSøker(
                    søkerAdresser = person.personopplysningGrunnlag.søker.bostedsadresser,
                    barnAdresser = person.bostedsadresser
            )
            GIFT_PARTNERSKAP -> VurderBarnErUgift(
                    sivilstander = person.sivilstander
            )
            BOSATT_I_RIKET -> VurderPersonErBosattIRiket(
                    adresser = person.bostedsadresser,
                    vurderFra = vurderFra
            )
            LOVLIG_OPPHOLD -> VurderPersonHarLovligOpphold()
        }

        return AutomatiskVurdering(
                evaluering = vilkårsregel.vurder(),
                regelInput = vilkårsregel.convertDataClassToJson()
        )
    }
}

data class AutomatiskVurdering(
        val regelInput: String,
        val evaluering: Evaluering,
        val resultat: Resultat = evaluering.resultat
)

data class GyldigVilkårsperiode(
        val gyldigFom: LocalDate = LocalDate.MIN,
        val gyldigTom: LocalDate = LocalDate.MAX
) {

    fun gyldigFor(dato: LocalDate): Boolean {
        return !(dato.isBefore(gyldigFom) || dato.isAfter(gyldigTom))
    }
}
