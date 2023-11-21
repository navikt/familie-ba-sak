package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse

data class BegrunnelseMedTriggere(
    val standardbegrunnelse: IVedtakBegrunnelse,
    val triggesAv: TriggesAv,
) {

    fun tilBrevBegrunnelseGrunnlagForLogging() =
        BrevBegrunnelseGrunnlagForLogging(
            standardbegrunnelse = this.standardbegrunnelse,
        )
}

fun Vedtaksbegrunnelse.tilBegrunnelseMedTriggere(
    sanityBegrunnelser: Map<Standardbegrunnelse, SanityBegrunnelse>,
): BegrunnelseMedTriggere {
    val sanityBegrunnelse =
        sanityBegrunnelser[this.standardbegrunnelse]
            ?: throw Feil("Finner ikke sanityBegrunnelse med apiNavn=${this.standardbegrunnelse.sanityApiNavn}")
    return BegrunnelseMedTriggere(
        standardbegrunnelse = this.standardbegrunnelse,
        triggesAv = sanityBegrunnelse.triggesAv,
    )
}
