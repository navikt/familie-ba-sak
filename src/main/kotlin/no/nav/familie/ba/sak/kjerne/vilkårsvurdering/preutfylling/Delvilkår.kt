package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

sealed class Delvilkår {
    open val begrunnelse: String = ""
}

data class OppfyltDelvilkår(
    override val begrunnelse: String,
) : Delvilkår()

data object IkkeOppfyltDelvilkår : Delvilkår()
