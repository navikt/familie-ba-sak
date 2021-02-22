package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.annenvurdering.AnnenVurdering
import no.nav.familie.ba.sak.annenvurdering.AnnenVurderingType
import no.nav.familie.ba.sak.nare.Resultat

data class RestAnnenVurdering(
        val resultat: Resultat,
        val type: AnnenVurderingType,
        val begrunnelse: String?
)

fun AnnenVurdering.tilRestAndreVurderinger() = RestAnnenVurdering(
        resultat = this.resultat,
        type = this.type,
        begrunnelse = this.begrunnelse
)