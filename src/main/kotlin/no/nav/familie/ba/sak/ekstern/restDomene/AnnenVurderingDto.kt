package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType

data class AnnenVurderingDto(
    val id: Long,
    val resultat: Resultat,
    val type: AnnenVurderingType,
    val begrunnelse: String?,
)

fun AnnenVurdering.tilAnnenVurderingDto() =
    AnnenVurderingDto(
        id = this.id,
        resultat = this.resultat,
        type = this.type,
        begrunnelse = this.begrunnelse,
    )
