package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.kjerne.brev.domene.eøs.EØSBegrunnelseMedTriggere
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import java.time.LocalDate

data class MinimertVedtaksperiode(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val begrunnelser: List<Standardbegrunnelse>,
    val eøsBegrunnelser: List<EØSBegrunnelseMedTriggere>,
    val fritekster: List<String> = emptyList(),
)

fun VedtaksperiodeMedBegrunnelser.tilMinimertVedtaksperiode(
    sanityEØSBegrunnelser: Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse>,
): MinimertVedtaksperiode {
    return MinimertVedtaksperiode(
        fom = this.fom,
        tom = this.tom,
        type = this.type,
        fritekster = this.fritekster.sortedBy { it.id }.map { it.fritekst },
        begrunnelser = this.begrunnelser.map { it.standardbegrunnelse },
        eøsBegrunnelser =
            this.eøsBegrunnelser.mapNotNull {
                it.begrunnelse.tilEØSBegrunnelseMedTriggere(
                    sanityEØSBegrunnelser,
                )
            },
    )
}
