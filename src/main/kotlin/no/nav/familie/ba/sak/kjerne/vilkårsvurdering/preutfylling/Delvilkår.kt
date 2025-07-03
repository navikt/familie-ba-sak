package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat.IKKE_OPPFYLT
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat.OPPFYLT

sealed class Delvilkår {
    open val begrunnelse: String = ""
    open val begrunnelseForManuellKontroll: BegrunnelseForManuellKontrollAvVilkår? = null

    fun tilResultat(): Resultat =
        when (this) {
            is OppfyltDelvilkår -> OPPFYLT
            is IkkeOppfyltDelvilkår -> IKKE_OPPFYLT
        }
}

data class OppfyltDelvilkår(
    override val begrunnelse: String,
    override val begrunnelseForManuellKontroll: BegrunnelseForManuellKontrollAvVilkår? = null,
) : Delvilkår()

data object IkkeOppfyltDelvilkår : Delvilkår()
