package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat

data class RestAnnenVurdering(
        val id: Long,
        val resultat: Resultat,
        val type: AnnenVurderingType,
        val begrunnelse: String?
)

fun AnnenVurdering.tilRestAnnenVurdering() = RestAnnenVurdering(
        id = this.id,
        resultat = this.resultat,
        type = this.type,
        begrunnelse = this.begrunnelse
)