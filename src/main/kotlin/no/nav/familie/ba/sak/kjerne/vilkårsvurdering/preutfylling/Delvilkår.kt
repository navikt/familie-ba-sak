package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

sealed class Delvilkår {
    open val begrunnelse: String = ""
    open val begrunnelseForManuellKontroll: BegrunnelseForManuellKontrollAvVilkår? = null
}

data class OppfyltDelvilkår(
    override val begrunnelse: String,
    override val begrunnelseForManuellKontroll: BegrunnelseForManuellKontrollAvVilkår? = null,
) : Delvilkår()

data object IkkeOppfyltDelvilkår : Delvilkår()
