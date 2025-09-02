package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

enum class BegrunnelseForManuellKontrollAvVilkår(
    private val beskrivelse: String,
) {
    INFORMASJON_FRA_SØKNAD("er fylt ut automatisk basert på informasjon fra søknaden"),
    INFORMASJON_OM_ARBEIDSFORHOLD("er fylt ut automatisk basert på informasjon om arbeidsforhold"),
    INFORMASJON_OM_OPPHOLDSTILLATELSE("er fylt ut automatisk basert på informasjon om oppholdstillatelse"),
    ;

    fun begrunnelse(vilkår: Vilkår) = "Vilkåret '${vilkår.beskrivelse}' $beskrivelse."
}
