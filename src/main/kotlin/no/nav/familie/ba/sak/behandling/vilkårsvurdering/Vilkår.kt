package no.nav.familie.ba.sak.behandling.vilkårsvurdering

import no.nav.familie.ba.sak.behandling.domene.vilkår.VilkårType
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.specifications.Spesifikasjon

class Vilkår(vilkårType: VilkårType, implementasjon: Fakta.() -> Evaluering) {
    val vilkårType = vilkårType
    val spesifikasjon = Spesifikasjon(
            beskrivelse = VilkårType.UNDER_18_ÅR_OG_BOR_MED_SØKER.beskrivelse,
            identifikator = VilkårType.UNDER_18_ÅR_OG_BOR_MED_SØKER.lovreferanse,
            implementasjon = implementasjon)

    fun evaluer(fakta: Fakta): Evaluering {
        return spesifikasjon.evaluer(fakta)
    }

    override fun toString(): String {
        return vilkårType.name
    }
}