package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.nare.core.specifications.Spesifikasjon
import java.time.LocalDate


enum class Vilkår(val parterDetteGjelderFor: List<PersonType>,
                  val spesifikasjon: Spesifikasjon<FaktaTilVilkårsvurdering>,
                  val begrunnelser: Map<BehandlingResultatType, List<BehandlingresultatOgVilkårBegrunnelse>> = emptyMap(),
                  val gyldigVilkårsperiode: GyldigVilkårsperiode) {

    UNDER_18_ÅR(
            parterDetteGjelderFor = listOf<PersonType>(PersonType.BARN),
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "Er under 18 år",
                    identifikator = "UNDER_18_ÅR",
                    implementasjon = { barnUnder18År(this) }),
            gyldigVilkårsperiode = GyldigVilkårsperiode()),
    BOR_MED_SØKER(
            parterDetteGjelderFor = listOf<PersonType>(PersonType.BARN),
            spesifikasjon = Spesifikasjon<FaktaTilVilkårsvurdering>(
                    beskrivelse = "Bor med søker: har samme adresse",
                    identifikator = "BOR_MED_SØKER:SAMME_ADRESSE",
                    implementasjon = { barnBorMedSøker(this) })
                    og Spesifikasjon(beskrivelse = "Bor med søker: har eksakt en søker",
                                     identifikator = "BOR_MED_SØKER:EN_SØKER",
                                     implementasjon = { harEnSøker(this) })
                    og Spesifikasjon(beskrivelse = "Bor med søker: søker må være mor",
                                     identifikator = "BOR_MED_SØKER:SØKER_ER_MOR",
                                     implementasjon = { søkerErMor(this) }),
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
            parterDetteGjelderFor = listOf<PersonType>(PersonType.BARN),
            spesifikasjon = Spesifikasjon(
                    beskrivelse = "Gift/partnerskap",
                    identifikator = "GIFT_PARTNERSKAP",
                    implementasjon = { giftEllerPartnerskap(this) }),
            gyldigVilkårsperiode = GyldigVilkårsperiode()),
    BOSATT_I_RIKET(
            parterDetteGjelderFor = listOf<PersonType>(PersonType.SØKER, PersonType.BARN),
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
            parterDetteGjelderFor = listOf<PersonType>(PersonType.SØKER, PersonType.BARN),
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

        fun hentFødselshendelseVilkårsreglerRekkefølge(): List<Pair<PersonType, Vilkår>> {
            return fødselshendelseVilkårsreglerRekkefølge
        }

        private val fødselshendelseVilkårsreglerRekkefølge = listOf(
                //Mor bosatt i riket
                Pair(PersonType.SØKER, Vilkår.BOSATT_I_RIKET),
                //Mor har lovlig opphold
                Pair(PersonType.SØKER, Vilkår.LOVLIG_OPPHOLD),
                //Barnet er under 18 år
                Pair(PersonType.BARN, Vilkår.UNDER_18_ÅR),
                //Barnet bor med søker
                Pair(PersonType.BARN, Vilkår.BOR_MED_SØKER),
                //Barnet er ugift og har ikke inngått partnerskap
                Pair(PersonType.BARN, Vilkår.GIFT_PARTNERSKAP),
                //Barnet er bosatt i riket
                Pair(PersonType.BARN, Vilkår.BOSATT_I_RIKET),
                //Barnet har lovlig opphold
                Pair(PersonType.BARN, Vilkår.LOVLIG_OPPHOLD),
        )
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


