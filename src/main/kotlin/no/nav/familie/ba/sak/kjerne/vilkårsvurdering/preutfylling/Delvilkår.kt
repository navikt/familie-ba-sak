package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.EvalueringÅrsak
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat.IKKE_OPPFYLT
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat.IKKE_VURDERT
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat.OPPFYLT
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering

sealed class Delvilkår {
    open val begrunnelse: String = ""
    open val begrunnelseForManuellKontroll: BegrunnelseForManuellKontrollAvVilkår? = null
    open val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList()
    open val evalueringÅrsaker: Set<EvalueringÅrsak> = emptySet()

    fun tilResultat(): Resultat =
        when (this) {
            is OppfyltDelvilkår -> OPPFYLT
            is IkkeOppfyltDelvilkår -> IKKE_OPPFYLT
            is IkkeVurdertVilkår -> IKKE_VURDERT
        }
}

data class IkkeVurdertVilkår(
    override val begrunnelse: String = "",
    override val evalueringÅrsaker: Set<EvalueringÅrsak> = emptySet(),
) : Delvilkår()

data class OppfyltDelvilkår(
    override val begrunnelse: String,
    override val begrunnelseForManuellKontroll: BegrunnelseForManuellKontrollAvVilkår? = null,
    override val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList(),
) : Delvilkår()

data class IkkeOppfyltDelvilkår(
    override val evalueringÅrsaker: Set<EvalueringÅrsak> = emptySet(),
    override val begrunnelse: String = "",
) : Delvilkår()
