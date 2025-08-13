package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat.IKKE_OPPFYLT
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat.OPPFYLT
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering

sealed class Delvilkår {
    open val begrunnelse: String = ""
    open val begrunnelseForManuellKontroll: BegrunnelseForManuellKontrollAvVilkår? = null
    open val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList()

    fun tilResultat(): Resultat =
        when (this) {
            is OppfyltDelvilkår -> OPPFYLT
            is IkkeOppfyltDelvilkår -> IKKE_OPPFYLT
        }
}

data class OppfyltDelvilkår(
    override val begrunnelse: String,
    override val begrunnelseForManuellKontroll: BegrunnelseForManuellKontrollAvVilkår? = null,
    override val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList(),
) : Delvilkår()

data object IkkeOppfyltDelvilkår : Delvilkår()
