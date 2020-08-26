package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.nare.core.specifications.Spesifikasjon
import java.time.LocalDate


enum class Vilkår(val parterDetteGjelderFor: List<PersonType>,
                  val spesifikasjon: Spesifikasjon<Fakta>,
                  val begrunnelser: Map<BehandlingResultatType, Map<VedtakBegrunnelse, Pair<String, (gjelderSøker: Boolean, barnasFødselsdatoer: String, vilkårsdato: String) -> String>>> = emptyMap(),
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
                            mapOf(
                                    VedtakBegrunnelse.INNVILGET_OMSORG_FOR_BARN
                                            to
                                            Pair(
                                                    "Adopsjon, surrogati: Omsorgen for barn",
                                                    { _, barnasFødselsdatoer, vilkårsdato -> "Du får barnetrygd fordi du har omsorgen for barn født $barnasFødselsdatoer fra $vilkårsdato." }
                                            ),
                                    VedtakBegrunnelse.INNVILGET_BOR_HOS_SØKER
                                            to
                                            Pair(
                                                    "Barn har flyttet til søker",
                                                    { _, barnasFødselsdatoer, vilkårsdato -> "Du får barnetrygd fordi barn født $barnasFødselsdatoer bor hos deg fra $vilkårsdato." }
                                            ),
                                    VedtakBegrunnelse.INNVILGET_FAST_OMSORG_FOR_BARN
                                            to
                                            Pair(
                                                    "Søker har fast omsorg for barn",
                                                    { _, barnasFødselsdatoer, vilkårsdato -> "Du får barnetrygd fordi vi har kommet fram til at du har fått fast omsorg for barn født $barnasFødselsdatoer fra $vilkårsdato." }
                                            )
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
                            mapOf(
                                    VedtakBegrunnelse.INNVILGET_BOSATT_I_RIKTET
                                            to
                                            Pair(
                                                    "Norsk, nordisk, tredjelandsborger med lovlig opphold samtidig som bosatt i Norge",
                                                    { gjelderSøker, barnasFødselsdatoer, vilkårsdato -> "Du får barnetrygd fordi${if (gjelderSøker && barnasFødselsdatoer.isNotBlank()) " du og " else if (gjelderSøker) " du " else " "}${if (barnasFødselsdatoer.isNotBlank()) "barn født $barnasFødselsdatoer" else ""} er bosatt i Norge fra $vilkårsdato." }
                                            )
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
                            mapOf(
                                    VedtakBegrunnelse.INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE
                                            to
                                            Pair(
                                                    "Tredjelandsborger bosatt før lovlig opphold i Norge",
                                                    { gjelderSøker, barnasFødselsdatoer, vilkårsdato -> "Du får barnetrygd fordi${if (gjelderSøker && barnasFødselsdatoer.isNotBlank()) " du og " else if (gjelderSøker) " du " else " "}${if (barnasFødselsdatoer.isNotBlank()) "barn født $barnasFødselsdatoer" else ""} har oppholdstillatelse fra $vilkårsdato." }
                                            ),
                                    VedtakBegrunnelse.INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER
                                            to
                                            Pair(
                                                    "EØS-borger: Søker har oppholdsrett",
                                                    { _, _, vilkårsdato -> "Du får barnetrygd fordi du har oppholdsrett som EØS-borger fra $vilkårsdato." }
                                            ),
                                    VedtakBegrunnelse.INNVILGET_LOVLIG_OPPHOLD_AAREG
                                            to
                                            Pair(
                                                    "EØS-borger: Søker/ektefelle/samboer arbeider eller har ytelser fra NAV",
                                                    { _, _, _ -> "Du får barnetrygd fordi du arbeider eller får utbetalinger fra NAV som er det samme som arbeidsinntekt." }
                                            )
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

enum class VedtakBegrunnelse {
    INNVILGET_BOSATT_I_RIKTET,
    INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE,
    INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER,
    INNVILGET_LOVLIG_OPPHOLD_AAREG,
    INNVILGET_OMSORG_FOR_BARN,
    INNVILGET_BOR_HOS_SØKER,
    INNVILGET_FAST_OMSORG_FOR_BARN,
}

data class GyldigVilkårsperiode(
        val gyldigFom: LocalDate = LocalDate.MIN,
        val gyldigTom: LocalDate = LocalDate.MAX
) {

    fun gyldigFor(dato: LocalDate): Boolean {
        return !(dato.isBefore(gyldigFom) || dato.isAfter(gyldigTom))
    }
}


