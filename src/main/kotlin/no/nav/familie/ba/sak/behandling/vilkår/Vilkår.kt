package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.common.Periode
import no.nav.nare.core.specifications.Spesifikasjon
import java.time.LocalDate

enum class Vilkår(val parterDetteGjelderFor: List<PersonType>,
                  val spesifikasjon: Spesifikasjon<Fakta>,
                  val genererPerioder: (fakta: Fakta, minLocalDate: LocalDate) -> List<Periode>) {

    UNDER_18_ÅR(
            parterDetteGjelderFor = listOf<PersonType>(PersonType.BARN),
            genererPerioder = { fakta, _ -> genererPerioderUnder18År(fakta) },
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "§2 - Er under 18 år",
                    identifikator = "UNDER_18_ÅR",
                    implementasjon = { barnUnder18År(this) })),
    BOR_MED_SØKER(
            parterDetteGjelderFor = listOf<PersonType>(PersonType.BARN),
            genererPerioder = { fakta, _ -> genererPerioderStandard(fakta) },
            spesifikasjon = Spesifikasjon<Fakta>(
                    beskrivelse = "§2-2 - Bor med søker: har samme adresse",
                    identifikator = "BOR_MED_SØKER:SAMME_ADRESSE",
                    implementasjon = { barnBorMedSøker(this) })
                    og Spesifikasjon(beskrivelse = "§2-2 - Bor med søker: har eksakt en søker",
                                     identifikator = "BOR_MED_SØKER:EN_SØKER",
                                     implementasjon = { harEnSøker(this) })
                    og Spesifikasjon(beskrivelse = "§2-2 - Bor med søker: søker må være mor",
                                     identifikator = "BOR_MED_SØKER:SØKER_ER_MOR",
                                     implementasjon = { søkerErMor(this) })),
    GIFT_PARTNERSKAP(
            parterDetteGjelderFor = listOf<PersonType>(PersonType.BARN),
            genererPerioder = { fakta, _ -> genererPerioderStandard(fakta) },
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "§2-4 - Gift/partnerskap",
                    identifikator = "GIFT_PARTNERSKAP",
                    implementasjon = { giftEllerPartnerskap(this) })),
    BOSATT_I_RIKET(
            parterDetteGjelderFor = listOf<PersonType>(PersonType.SØKER, PersonType.BARN),
            genererPerioder = { fakta, _ -> genererPerioderStandard(fakta) },
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "§4-1 - Bosatt i riket",
                    identifikator = "BOSATT_I_RIKET",
                    implementasjon = { bosattINorge(this) })),
    LOVLIG_OPPHOLD(
            parterDetteGjelderFor = listOf<PersonType>(PersonType.SØKER, PersonType.BARN),
            genererPerioder = { fakta, minLocalDate -> genererPerioderLovligOpphold(fakta, minLocalDate) },
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "§4-2 - Lovlig opphold",
                    identifikator = "LOVLIG_OPPHOLD",
                    implementasjon = { lovligOpphold(this) }));

    override fun toString(): String {
        return this.spesifikasjon.beskrivelse
    }

    companion object {
        fun hentVilkårFor(personType: PersonType): Set<Vilkår> {
            return values().filter {
                personType in it.parterDetteGjelderFor
            }.toSet()
        }
    }
}


data class GyldigVilkårsperiode(
        val gyldigFom: LocalDate = LocalDate.MIN,
        val gyldigTom: LocalDate = LocalDate.MAX
) {

    fun gyldigFor(dato: LocalDate): Boolean {
        return !(dato.isBefore(gyldigFom) || dato.isAfter(gyldigTom))
    }
}


