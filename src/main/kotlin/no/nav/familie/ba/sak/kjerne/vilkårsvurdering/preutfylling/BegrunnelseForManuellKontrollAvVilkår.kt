package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

enum class BegrunnelseForManuellKontrollAvVilkår(
    private val beskrivelse: String,
) {
    INFORMASJON_FRA_SØKNAD("er fylt ut automatisk basert på informasjon fra søknaden"),
    INFORMASJON_OM_ARBEIDSFORHOLD("er fylt ut automatisk basert på informasjon om arbeidsforhold"),
    INFORMASJON_OM_OPPHOLDSTILLATELSE("er fylt ut automatisk basert på informasjon om oppholdstillatelse"),
    INFORMASJON_OM_DELT_BOSTED("er fylt ut automatisk basert på informasjon om delt bosted"),
    SØKER_HAR_OPPGITT_AT_BARNET_IKKE_BOR_FAST_MED_SØKER("er ikke fylt ut automatisk fordi søker har oppgitt i søknaden at barnet ikke bor fast med søker"),
    ;

    fun begrunnelse(vilkår: Vilkår) = "Vilkåret '${vilkår.beskrivelse}' $beskrivelse."
}
