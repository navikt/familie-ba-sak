package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Utils.slåSammen
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import java.time.LocalDate

fun List<LocalDate>.tilBrevTekst(): String = this.sorted().map { it.tilKortString() }.slåSammen()

fun Standardbegrunnelse.tilVedtaksbegrunnelse(
    vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
): Vedtaksbegrunnelse {
    if (!vedtaksperiodeMedBegrunnelser
            .type
            .tillatteBegrunnelsestyper
            .contains(this.vedtakBegrunnelseType)
    ) {
        throw Feil(
            "Begrunnelsestype ${this.vedtakBegrunnelseType} passer ikke med " +
                "typen '${vedtaksperiodeMedBegrunnelser.type}' som er satt på perioden.",
        )
    }

    return Vedtaksbegrunnelse(
        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
        standardbegrunnelse = this,
    )
}
