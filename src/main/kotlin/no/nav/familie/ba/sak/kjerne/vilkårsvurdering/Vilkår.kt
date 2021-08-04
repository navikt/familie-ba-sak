package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.vilkårsvurdering.vurderBarnetErBosattMedSøker
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.vilkårsvurdering.vurderPersonErBosattIRiket
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.vilkårsvurdering.vurderPersonErUgift
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.vilkårsvurdering.vurderPersonErUnder18
import no.nav.familie.ba.sak.kjerne.automatiskvurdering.vilkårsvurdering.vurderPersonHarLovligOpphold
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Evaluering
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
                  val vurder: Person.() -> AutomatiskVurdering,
                  @Deprecated("Erstattes av enklere løsning med vurder")
                  val spesifikasjon: Spesifikasjon<FaktaTilVilkårsvurdering>) {

    UNDER_18_ÅR(
            parterDetteGjelderFor = listOf(BARN),
            vurder = { hentResultatVilkårUnder18(this) },
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "Er under 18 år",
                    identifikator = "UNDER_18_ÅR",
                    implementasjon = { barnUnder18År(this) })),
    BOR_MED_SØKER(
            parterDetteGjelderFor = listOf(BARN),
            vurder = { hentResultatVilkårBorMedSøker(this) },
            spesifikasjon = Spesifikasjon<FaktaTilVilkårsvurdering>(
                    beskrivelse = "Bor med søker",
                    identifikator = "BOR_MED_SØKER",
                    implementasjon = {
                        søkerErMor(this) og barnBorMedSøker(this)
                    }
            )),

    GIFT_PARTNERSKAP(
            parterDetteGjelderFor = listOf(BARN),
            vurder = { hentResultatVilkårGiftPartnerskap(this) },
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "Gift/partnerskap",
                    identifikator = "GIFT_PARTNERSKAP",
                    implementasjon = { giftEllerPartnerskap(this) })),
    BOSATT_I_RIKET(
            parterDetteGjelderFor = listOf(SØKER, BARN),
            vurder = { hentResultatVilkårBosattIRiket(this) },
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "Bosatt i riket",
                    identifikator = "BOSATT_I_RIKET",
                    implementasjon = { bosattINorge(this) })),
    LOVLIG_OPPHOLD(
            parterDetteGjelderFor = listOf(SØKER, BARN),
            vurder = { hentResultatVilkårLovligOpphold(this) },
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
