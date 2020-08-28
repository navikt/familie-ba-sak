package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.nare.core.specifications.Spesifikasjon
import java.time.LocalDate


enum class Vilkår(val parterDetteGjelderFor: List<PersonType>,
                  val spesifikasjon: Spesifikasjon<Fakta>,
                  val begrunnelser: Map<BehandlingResultatType, List<VedtakBegrunnelse>> = emptyMap(),
                  val gyldigVilkårsperiode: GyldigVilkårsperiode) {

    UNDER_18_ÅR(
            parterDetteGjelderFor = listOf<PersonType>(PersonType.BARN),
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "§2 - Er under 18 år",
                    identifikator = "UNDER_18_ÅR",
                    implementasjon = { barnUnder18År(this) }),
            gyldigVilkårsperiode = GyldigVilkårsperiode()),
    BOR_MED_SØKER(
            parterDetteGjelderFor = listOf<PersonType>(PersonType.BARN),
            spesifikasjon = Spesifikasjon<Fakta>(
                    beskrivelse = "§2-2 - Bor med søker: har samme adresse",
                    identifikator = "BOR_MED_SØKER:SAMME_ADRESSE",
                    implementasjon = { barnBorMedSøker(this) })
                    og Spesifikasjon(beskrivelse = "§2-2 - Bor med søker: har eksakt en søker",
                                     identifikator = "BOR_MED_SØKER:EN_SØKER",
                                     implementasjon = { harEnSøker(this) })
                    og Spesifikasjon(beskrivelse = "§2-2 - Bor med søker: søker må være mor",
                                     identifikator = "BOR_MED_SØKER:SØKER_ER_MOR",
                                     implementasjon = { søkerErMor(this) }),
            begrunnelser = mapOf(
                    BehandlingResultatType.INNVILGET
                            to
                            listOf(
                                    VedtakBegrunnelse.INNVILGET_OMSORG_FOR_BARN,
                                    VedtakBegrunnelse.INNVILGET_BOR_HOS_SØKER,
                                    VedtakBegrunnelse.INNVILGET_FAST_OMSORG_FOR_BARN
                            )
            ),
            gyldigVilkårsperiode = GyldigVilkårsperiode()),
    GIFT_PARTNERSKAP(
            parterDetteGjelderFor = listOf<PersonType>(PersonType.BARN),
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "§2-4 - Gift/partnerskap",
                    identifikator = "GIFT_PARTNERSKAP",
                    implementasjon = { giftEllerPartnerskap(this) }),
            gyldigVilkårsperiode = GyldigVilkårsperiode()),
    BOSATT_I_RIKET(
            parterDetteGjelderFor = listOf<PersonType>(PersonType.SØKER, PersonType.BARN),
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "§4-1 - Bosatt i riket",
                    identifikator = "BOSATT_I_RIKET",
                    implementasjon = { bosattINorge(this) }),
            begrunnelser = mapOf(
                    BehandlingResultatType.INNVILGET
                            to
                            listOf(
                                    VedtakBegrunnelse.INNVILGET_BOSATT_I_RIKTET
                            )
            ),
            gyldigVilkårsperiode = GyldigVilkårsperiode()),
    LOVLIG_OPPHOLD(
            parterDetteGjelderFor = listOf<PersonType>(PersonType.SØKER, PersonType.BARN),
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "§4-2 - Lovlig opphold",
                    identifikator = "LOVLIG_OPPHOLD",
                    implementasjon =
                    { lovligOpphold(this) }),
            begrunnelser = mapOf(
                    BehandlingResultatType.INNVILGET
                            to
                            listOf(
                                    VedtakBegrunnelse.INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE,
                                    VedtakBegrunnelse.INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER,
                                    VedtakBegrunnelse.INNVILGET_LOVLIG_OPPHOLD_AAREG
                            )
            ),
            gyldigVilkårsperiode = GyldigVilkårsperiode());

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


