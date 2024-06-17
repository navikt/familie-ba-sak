package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene

import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilRestVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import java.time.LocalDate

data class RestUtvidetVedtaksperiodeMedBegrunnelser(
    val id: Long,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val begrunnelser: List<RestVedtaksbegrunnelse>,
    val fritekster: List<String> = emptyList(),
    val gyldigeBegrunnelser: List<String>,
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj> = emptyList(),
)

fun UtvidetVedtaksperiodeMedBegrunnelser.tilRestUtvidetVedtaksperiodeMedBegrunnelser(
    sanityBegrunnelser: List<SanityBegrunnelse>,
    sanityEØSBegrunnelser: List<SanityEØSBegrunnelse>,
): RestUtvidetVedtaksperiodeMedBegrunnelser =
    RestUtvidetVedtaksperiodeMedBegrunnelser(
        id = this.id,
        fom = this.fom,
        tom = this.tom,
        type = this.type,
        begrunnelser = this.begrunnelser.map { it.tilRestVedtaksbegrunnelse(sanityBegrunnelser) } + this.eøsBegrunnelser.map { it.tilRestVedtaksbegrunnelse(sanityEØSBegrunnelser) },
        fritekster = this.fritekster,
        utbetalingsperiodeDetaljer = this.utbetalingsperiodeDetaljer,
        gyldigeBegrunnelser = this.gyldigeBegrunnelser.map { it.enumnavnTilString() },
    )
