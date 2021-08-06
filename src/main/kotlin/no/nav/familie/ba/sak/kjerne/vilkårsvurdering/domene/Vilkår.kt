package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering.vurderBarnetErBosattMedSøker
import no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering.vurderPersonErBosattIRiket
import no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering.vurderPersonErUgift
import no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering.vurderPersonErUnder18
import no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering.vurderPersonHarLovligOpphold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType.BARN
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType.SØKER
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse.Companion.sisteAdresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand.Companion.sisteSivilstand
import java.time.LocalDate


enum class Vilkår(val parterDetteGjelderFor: List<PersonType>,
                  val beskrivelse: String,
                  val vurder: Person.() -> AutomatiskVurdering) {

    UNDER_18_ÅR(
            parterDetteGjelderFor = listOf(BARN),
            beskrivelse = "Er under 18 år",
            vurder = { hentResultatVilkårUnder18(this) }),
    BOR_MED_SØKER(
            parterDetteGjelderFor = listOf(BARN),
            beskrivelse = "Bor med søker",
            vurder = { hentResultatVilkårBorMedSøker(this) }),

    GIFT_PARTNERSKAP(
            parterDetteGjelderFor = listOf(BARN),
            beskrivelse = "Gift/partnerskap",
            vurder = { hentResultatVilkårGiftPartnerskap(this) }),
    BOSATT_I_RIKET(
            parterDetteGjelderFor = listOf(SØKER, BARN),
            beskrivelse = "Bosatt i riket",
            vurder = { hentResultatVilkårBosattIRiket(this) }),
    LOVLIG_OPPHOLD(
            parterDetteGjelderFor = listOf(SØKER, BARN),
            beskrivelse = "Lovlig opphold",
            vurder = { hentResultatVilkårLovligOpphold(this) });

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
}

internal fun hentResultatVilkårUnder18(person: Person): AutomatiskVurdering {
    return AutomatiskVurdering(
            regelInput = person.convertDataClassToJson(),
            evaluering = vurderPersonErUnder18(person.hentAlder())
    )
}

internal fun hentResultatVilkårBorMedSøker(person: Person): AutomatiskVurdering {
    return AutomatiskVurdering(
            regelInput = person.convertDataClassToJson(),
            evaluering = vurderBarnetErBosattMedSøker(
                    person.bostedsadresser.sisteAdresse(),
                    person.personopplysningGrunnlag.søker.bostedsadresser.sisteAdresse()
            )
    )
}

internal fun hentResultatVilkårGiftPartnerskap(person: Person): AutomatiskVurdering {
    return AutomatiskVurdering(regelInput = person.convertDataClassToJson(),
                               evaluering = vurderPersonErUgift(person.sivilstander.sisteSivilstand()))
}

internal fun hentResultatVilkårBosattIRiket(person: Person): AutomatiskVurdering {
    return AutomatiskVurdering(regelInput = person.convertDataClassToJson(),
                               evaluering = vurderPersonErBosattIRiket(person.bostedsadresser.sisteAdresse()))
}

internal fun hentResultatVilkårLovligOpphold(person: Person): AutomatiskVurdering {
    return AutomatiskVurdering(regelInput = person.convertDataClassToJson(), evaluering = vurderPersonHarLovligOpphold())
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
