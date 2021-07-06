package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.barnetErBosattMedSøker
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.borMedSøkerRegelInput
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.bosattIRiketRegelInput
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.giftEllerPartnerskapRegelInput
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.personErBosattIRiket
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.personErUgift
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.personErUnder18
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.personHarLovligOpphold
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.under18RegelInput
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

internal fun convertDataClassToJson(T: Any): String {
    return jacksonObjectMapper().writeValueAsString(T)
}

fun Vilkår.vurder(person: Person): Pair<String, Resultat> {
    return when (this) {
        Vilkår.UNDER_18_ÅR -> under18Input(person)

        Vilkår.BOR_MED_SØKER -> borMedSøkerInput(person)

        Vilkår.GIFT_PARTNERSKAP -> giftPartnerskapInput(person)

        Vilkår.BOSATT_I_RIKET -> bosattIRiket(person)

        Vilkår.LOVLIG_OPPHOLD -> lovligOpphold(person)
    }
}

internal fun under18Input(person: Person): Pair<String, Resultat> {
    return Pair(convertDataClassToJson(under18RegelInput(LocalDate.now(), person.fødselsdato)),
                personErUnder18(person.fødselsdato))
}

internal fun borMedSøkerInput(person: Person): Pair<String, Resultat> {
    return Pair(convertDataClassToJson(borMedSøkerRegelInput(person.bostedsadresser.sisteAdresse(),
                                                             person.personopplysningGrunnlag.søker.bostedsadresser.sisteAdresse())),
                barnetErBosattMedSøker(person.bostedsadresser.sisteAdresse(),
                                       person.personopplysningGrunnlag.søker.bostedsadresser.sisteAdresse()))
}

internal fun giftPartnerskapInput(person: Person): Pair<String, Resultat> {
    return Pair(convertDataClassToJson(giftEllerPartnerskapRegelInput(person.sivilstander.sisteSivilstand())),
                personErUgift(person.sivilstander.sisteSivilstand()))
}

internal fun bosattIRiket(person: Person): Pair<String, Resultat> {
    return Pair(convertDataClassToJson(bosattIRiketRegelInput(person.bostedsadresser.sisteAdresse())),
                personErBosattIRiket(person.bostedsadresser.sisteAdresse()))
}

internal fun lovligOpphold(person: Person): Pair<String, Resultat> {
    return Pair("Alltid lovlig opphold i sommercase", personHarLovligOpphold())
}

data class GyldigVilkårsperiode(
        val gyldigFom: LocalDate = LocalDate.MIN,
        val gyldigTom: LocalDate = LocalDate.MAX
) {

    fun gyldigFor(dato: LocalDate): Boolean {
        return !(dato.isBefore(gyldigFom) || dato.isAfter(gyldigTom))
    }
}
