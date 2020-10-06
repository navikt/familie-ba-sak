package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType.BARN
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType.SØKER
import no.nav.familie.ba.sak.nare.Spesifikasjon
import java.time.LocalDate


enum class Vilkår(val parterDetteGjelderFor: List<PersonType>,
                  val spesifikasjon: Spesifikasjon<FaktaTilVilkårsvurdering>,
                  val begrunnelser: Map<BehandlingResultatType, List<BehandlingresultatOgVilkårBegrunnelse>> = emptyMap(),
                  val gyldigVilkårsperiode: GyldigVilkårsperiode) {

    UNDER_18_ÅR(
            parterDetteGjelderFor = listOf<PersonType>(BARN),
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "Er under 18 år",
                    identifikator = "UNDER_18_ÅR",
                    implementasjon = { barnUnder18År(this) }),
            gyldigVilkårsperiode = GyldigVilkårsperiode()),
    BOR_MED_SØKER(
            parterDetteGjelderFor = listOf<PersonType>(BARN),
            spesifikasjon = Spesifikasjon<FaktaTilVilkårsvurdering>(
                    beskrivelse = "Bor med søker",
                    identifikator = "BOR_MED_SØKER",
                    implementasjon = {
                        søkerErMor(this) og barnBorMedSøker(this)
                    }
            ),
            begrunnelser = mapOf(
                    BehandlingResultatType.INNVILGET
                            to
                            listOf(
                                    BehandlingresultatOgVilkårBegrunnelse.INNVILGET_OMSORG_FOR_BARN,
                                    BehandlingresultatOgVilkårBegrunnelse.INNVILGET_BOR_HOS_SØKER,
                                    BehandlingresultatOgVilkårBegrunnelse.INNVILGET_FAST_OMSORG_FOR_BARN
                            )
            ),
            gyldigVilkårsperiode = GyldigVilkårsperiode()),
    GIFT_PARTNERSKAP(
            parterDetteGjelderFor = listOf<PersonType>(BARN),
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "Gift/partnerskap",
                    identifikator = "GIFT_PARTNERSKAP",
                    implementasjon = { giftEllerPartnerskap(this) }),
            gyldigVilkårsperiode = GyldigVilkårsperiode()),
    BOSATT_I_RIKET(
            parterDetteGjelderFor = listOf<PersonType>(SØKER, BARN),
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "Bosatt i riket",
                    identifikator = "BOSATT_I_RIKET",
                    implementasjon = { bosattINorge(this) }),
            begrunnelser = mapOf(
                    BehandlingResultatType.INNVILGET
                            to
                            listOf(
                                    BehandlingresultatOgVilkårBegrunnelse.INNVILGET_BOSATT_I_RIKTET
                            )
            ),
            gyldigVilkårsperiode = GyldigVilkårsperiode()),
    LOVLIG_OPPHOLD(
            parterDetteGjelderFor = listOf<PersonType>(SØKER, BARN),
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "Lovlig opphold",
                    identifikator = "LOVLIG_OPPHOLD",
                    implementasjon =
                    { lovligOpphold(this) }),
            begrunnelser = mapOf(
                    BehandlingResultatType.INNVILGET
                            to
                            listOf(
                                    BehandlingresultatOgVilkårBegrunnelse.INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE,
                                    BehandlingresultatOgVilkårBegrunnelse.INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER,
                                    BehandlingresultatOgVilkårBegrunnelse.INNVILGET_LOVLIG_OPPHOLD_AAREG
                            )
            ),
            gyldigVilkårsperiode = GyldigVilkårsperiode());

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

data class GyldigVilkårsperiode(
        val gyldigFom: LocalDate = LocalDate.MIN,
        val gyldigTom: LocalDate = LocalDate.MAX
) {

    fun gyldigFor(dato: LocalDate): Boolean {
        return !(dato.isBefore(gyldigFom) || dato.isAfter(gyldigTom))
    }
}


