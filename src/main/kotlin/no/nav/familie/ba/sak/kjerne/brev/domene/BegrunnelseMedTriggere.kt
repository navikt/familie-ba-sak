package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse

data class BegrunnelseMedTriggere(
    val standardbegrunnelse: IVedtakBegrunnelse,
) {

    fun tilBrevBegrunnelseGrunnlagForLogging() =
        BrevBegrunnelseGrunnlagForLogging(
            standardbegrunnelse = this.standardbegrunnelse,
        )
}

fun Vedtaksbegrunnelse.tilBegrunnelseMedTriggere(): BegrunnelseMedTriggere {
    return BegrunnelseMedTriggere(
        standardbegrunnelse = this.standardbegrunnelse,
    )
}
