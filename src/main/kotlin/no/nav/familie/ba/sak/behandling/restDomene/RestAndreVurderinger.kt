package no.nav.familie.ba.sak.behandling.restDomene

import no.nav.familie.ba.sak.andreopplysninger.AndreVurderinger
import no.nav.familie.ba.sak.andreopplysninger.AndreVurderingerType
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.opplysningsplikt.Opplysningsplikt
import no.nav.familie.ba.sak.opplysningsplikt.OpplysningspliktStatus

data class RestAndreVurderinger(
        val resultat: Resultat,
        val type: AndreVurderingerType,
        val begrunnelse: String?
)

fun AndreVurderinger.tilRestAndreVurderinger() = RestAndreVurderinger(
        resultat = this.resultat,
        type = this.type,
        begrunnelse = this.begrunnelse
)