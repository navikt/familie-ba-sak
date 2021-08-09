package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene

import no.nav.familie.ba.sak.common.convertDataClassToJson
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering.VilkårsvurderingFakta
import no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering.vurderBarnetErBosattMedSøker
import no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering.vurderPersonErBosattIRiket
import no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering.vurderPersonErUgift
import no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering.vurderPersonErUnder18
import no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering.vurderPersonHarLovligOpphold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType.BARN
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType.SØKER
import java.time.LocalDate


enum class Vilkår(val parterDetteGjelderFor: List<PersonType>,
                  val beskrivelse: String,
                  val vurder: VilkårsvurderingFakta.() -> AutomatiskVurdering) {

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

internal fun hentResultatVilkårUnder18(fakta: VilkårsvurderingFakta): AutomatiskVurdering {
    return AutomatiskVurdering(
            regelInput = fakta.convertDataClassToJson(),
            evaluering = vurderPersonErUnder18(fakta.person.hentAlder())
    )
}

internal fun hentResultatVilkårBorMedSøker(fakta: VilkårsvurderingFakta): AutomatiskVurdering {
    return AutomatiskVurdering(
            regelInput = fakta.convertDataClassToJson(),
            evaluering = vurderBarnetErBosattMedSøker(
                    fakta.person.bostedsadresser,
                    fakta.person.personopplysningGrunnlag.søker.bostedsadresser
            )
    )
}

internal fun hentResultatVilkårGiftPartnerskap(fakta: VilkårsvurderingFakta): AutomatiskVurdering {
    return AutomatiskVurdering(regelInput = fakta.convertDataClassToJson(),
                               evaluering = vurderPersonErUgift(fakta.person.sivilstander))
}

internal fun hentResultatVilkårBosattIRiket(fakta: VilkårsvurderingFakta): AutomatiskVurdering {
    return AutomatiskVurdering(regelInput = fakta.convertDataClassToJson(),
                               evaluering = vurderPersonErBosattIRiket(fakta.person.bostedsadresser, fakta.vurderFra))
}

internal fun hentResultatVilkårLovligOpphold(fakta: VilkårsvurderingFakta): AutomatiskVurdering {
    return AutomatiskVurdering(regelInput = fakta.convertDataClassToJson(), evaluering = vurderPersonHarLovligOpphold())
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
