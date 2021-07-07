package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.borMedSøkerRegelInput
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.bosattIRiketRegelInput
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.giftEllerPartnerskapRegelInput
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.under18RegelInput
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.vurderBarnetErBosattMedSøker
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.vurderPersonErBosattIRiket
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.vurderPersonErUgift
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.vurderPersonErUnder18
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.vurderPersonHarLovligOpphold
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Spesifikasjon
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType.BARN
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType.SØKER
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse.Companion.sisteAdresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand.Companion.sisteSivilstand
import java.time.LocalDate


enum class Vilkår(val parterDetteGjelderFor: List<PersonType>,
                  val spesifikasjon: Spesifikasjon<FaktaTilVilkårsvurdering>) {

    UNDER_18_ÅR(
            parterDetteGjelderFor = listOf(BARN),
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "Er under 18 år",
                    identifikator = "UNDER_18_ÅR",
                    implementasjon = { barnUnder18År(this) })),
    BOR_MED_SØKER(
            parterDetteGjelderFor = listOf(BARN),
            spesifikasjon = Spesifikasjon<FaktaTilVilkårsvurdering>(
                    beskrivelse = "Bor med søker",
                    identifikator = "BOR_MED_SØKER",
                    implementasjon = {
                        søkerErMor(this) og barnBorMedSøker(this)
                    }
            )),
    GIFT_PARTNERSKAP(
            parterDetteGjelderFor = listOf(BARN),
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "Gift/partnerskap",
                    identifikator = "GIFT_PARTNERSKAP",
                    implementasjon = { giftEllerPartnerskap(this) })),
    BOSATT_I_RIKET(
            parterDetteGjelderFor = listOf(SØKER, BARN),
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "Bosatt i riket",
                    identifikator = "BOSATT_I_RIKET",
                    implementasjon = { bosattINorge(this) })),
    LOVLIG_OPPHOLD(
            parterDetteGjelderFor = listOf(SØKER, BARN),
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "Lovlig opphold",
                    identifikator = "LOVLIG_OPPHOLD",
                    implementasjon =
                    { lovligOpphold(this) }));

    override fun toString(): String {
        return this.spesifikasjon.beskrivelse
    }

    companion object {

        fun hentSamletSpesifikasjonForPerson(personType: PersonType): Spesifikasjon<FaktaTilVilkårsvurdering> {
            return hentVilkårFor(personType)
                    .toSet()
                    .map { vilkår -> vilkår.spesifikasjon }
                    .reduce { samledeVilkårsregler, vilkårsregler -> samledeVilkårsregler og vilkårsregler }
        }

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

internal fun convertDataClassToJson(dataklasse: Any): String {
    return jacksonObjectMapper().writeValueAsString(dataklasse)
}

fun Vilkår.vurder(person: Person): Pair<String, Resultat> {
    return when (this) {
        Vilkår.UNDER_18_ÅR -> vurderVilkårUnder18(person)

        Vilkår.BOR_MED_SØKER -> vurderVilkårBorMedSøker(person)

        Vilkår.GIFT_PARTNERSKAP -> vurderVilkårGiftPartnerskap(person)

        Vilkår.BOSATT_I_RIKET -> vurderVilkårBosattIRiket(person)

        Vilkår.LOVLIG_OPPHOLD -> vurderVilkårLovligOpphold(person)
    }
}

internal fun vurderVilkårUnder18(person: Person): Pair<String, Resultat> {
    return Pair(convertDataClassToJson(under18RegelInput(LocalDate.now(), person.fødselsdato)),
                vurderPersonErUnder18(person.fødselsdato))
}

internal fun vurderVilkårBorMedSøker(person: Person): Pair<String, Resultat> {
    return Pair(convertDataClassToJson(borMedSøkerRegelInput(person.bostedsadresser.sisteAdresse(),
                                                             person.personopplysningGrunnlag.søker.bostedsadresser.sisteAdresse())),
                vurderBarnetErBosattMedSøker(person.bostedsadresser.sisteAdresse(),
                                             person.personopplysningGrunnlag.søker.bostedsadresser.sisteAdresse()))
}

internal fun vurderVilkårGiftPartnerskap(person: Person): Pair<String, Resultat> {
    return Pair(convertDataClassToJson(giftEllerPartnerskapRegelInput(person.sivilstander.sisteSivilstand())),
                vurderPersonErUgift(person.sivilstander.sisteSivilstand()))
}

internal fun vurderVilkårBosattIRiket(person: Person): Pair<String, Resultat> {
    return Pair(convertDataClassToJson(bosattIRiketRegelInput(person.bostedsadresser.sisteAdresse())),
                vurderPersonErBosattIRiket(person.bostedsadresser.sisteAdresse()))
}

internal fun vurderVilkårLovligOpphold(person: Person): Pair<String, Resultat> {
    return Pair("Alltid lovlig opphold i sommercase", vurderPersonHarLovligOpphold())
}

data class GyldigVilkårsperiode(
        val gyldigFom: LocalDate = LocalDate.MIN,
        val gyldigTom: LocalDate = LocalDate.MAX
) {

    fun gyldigFor(dato: LocalDate): Boolean {
        return !(dato.isBefore(gyldigFom) || dato.isAfter(gyldigTom))
    }
}
