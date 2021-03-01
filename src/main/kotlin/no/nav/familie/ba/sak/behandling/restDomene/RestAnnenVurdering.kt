package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.annenvurdering.AnnenVurdering
import no.nav.familie.ba.sak.annenvurdering.AnnenVurderingType
import no.nav.familie.ba.sak.nare.Resultat

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