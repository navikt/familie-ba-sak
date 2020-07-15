package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.restDomene.SøknadDTO
import no.nav.familie.ba.sak.behandling.restDomene.TypeSøker
import no.nav.nare.core.specifications.Spesifikasjon
import java.time.LocalDate


enum class Vilkår(val parterDetteGjelderFor: List<PersonType>,
                  val gjelderKunFor: List<SakType> = emptyList(),
                  val spesifikasjon: Spesifikasjon<Fakta>,
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
                    beskrivelse = "§2-2 - Bor med søker",
                    identifikator = "BOR_MED_SØKER",
                    implementasjon = { barnBorMedSøker(this) })
                    og Spesifikasjon(beskrivelse = "§2-2 - Bor med søker: Har eksakt en søker",
                                     identifikator = "BOR_MED_SØKER",
                                     implementasjon = { harEnSøker(this) })
                    og Spesifikasjon(beskrivelse = "§2-2 - Bor med søker: søker må være mor",
                                     identifikator = "BOR_MED_SØKER",
                                     implementasjon = { søkerErMor(this) }),
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
            gyldigVilkårsperiode = GyldigVilkårsperiode()),
    LOVLIG_OPPHOLD(
            parterDetteGjelderFor = listOf<PersonType>(PersonType.SØKER, PersonType.BARN),
            gjelderKunFor = listOf<SakType>(SakType.EØS, SakType.TREDJELANDSBORGER),
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "§4-2 - Lovlig opphold",
                    identifikator = "LOVLIG_OPPHOLD",
                    implementasjon = { lovligOpphold(this) }),
            gyldigVilkårsperiode = GyldigVilkårsperiode());

    override fun toString(): String {
        return this.spesifikasjon.beskrivelse
    }

    companion object {
        fun hentVilkårForPart(personType: PersonType) = values()
                .filter { personType in it.parterDetteGjelderFor }.toSet()

        fun hentVilkårForPart(personType: PersonType, vurderingsDato: LocalDate) = values()
                .filter {
                    personType in it.parterDetteGjelderFor
                    && it.gyldigVilkårsperiode.gyldigFor(vurderingsDato)
                }.toSet()

        fun hentVilkårForSakstype(sakstype: SakType) = values()
                .filter { it.gjelderKunFor.isEmpty() || sakstype in it.gjelderKunFor }.toSet()

        fun hentVilkårFor(personType: PersonType, sakstype: SakType): Set<Vilkår> {
            return values().filter {
                personType in it.parterDetteGjelderFor
                && (it.gjelderKunFor.isEmpty() || sakstype in it.gjelderKunFor)
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

enum class SakType {
    EØS, NASJONAL, TREDJELANDSBORGER;

    companion object {
        fun valueOfType(type: Any): SakType {
            return when (type) {
                BehandlingKategori.EØS -> EØS
                BehandlingKategori.NASJONAL -> NASJONAL
                else -> throw IllegalArgumentException("Finner ingen mapping til SakType for $type")
            }
        }

        fun hentSakType(behandlingKategori: BehandlingKategori, søknadDTO: SøknadDTO?): SakType {
            return if (behandlingKategori == BehandlingKategori.NASJONAL &&
                       (søknadDTO?.typeSøker == TypeSøker.TREDJELANDSBORGER || søknadDTO?.typeSøker == TypeSøker.EØS_BORGER)) {
                TREDJELANDSBORGER
            } else valueOfType(behandlingKategori)
        }
    }
}

