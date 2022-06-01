package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene

import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.EØSBegrunnelseMedTriggere
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.finnBegrunnelse

data class RestEØSVedtaksbegrunnelse(
    val eøsStandardbegrunnelse: EØSStandardbegrunnelse,
    val vedtakBegrunnelseType: VedtakBegrunnelseType,
) {
    fun tilEØSBegrunnelseMedTriggere(sanityEØSBegrunnelser: List<SanityEØSBegrunnelse>): EØSBegrunnelseMedTriggere? {
        val sanityEØSBegrunnelse = sanityEØSBegrunnelser.finnBegrunnelse(this.eøsStandardbegrunnelse) ?: return null
        return EØSBegrunnelseMedTriggere(
            eøsBegrunnelse = this.eøsStandardbegrunnelse,
            sanityEØSBegrunnelse = sanityEØSBegrunnelse
        )
    }
}
