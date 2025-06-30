package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

interface Delvilkår {
    val begrunnelse: String
}

data class OppfyltDelvilkår(
    override val begrunnelse: String,
) : Delvilkår

data object IkkeOppfyltDelvilkår : Delvilkår {
    override val begrunnelse: String = ""
}
