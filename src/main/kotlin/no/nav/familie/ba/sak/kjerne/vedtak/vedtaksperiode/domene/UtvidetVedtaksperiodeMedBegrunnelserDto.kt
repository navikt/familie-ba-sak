package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.domene

import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilVedtaksbegrunnelseDto
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.UtbetalingsperiodeDetalj
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import java.time.LocalDate

data class UtvidetVedtaksperiodeMedBegrunnelserDto(
    val id: Long,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: Vedtaksperiodetype,
    val begrunnelser: List<VedtaksbegrunnelseDto>,
    val fritekster: List<String> = emptyList(),
    val gyldigeBegrunnelser: List<String>,
    val utbetalingsperiodeDetaljer: List<UtbetalingsperiodeDetalj> = emptyList(),
)

fun UtvidetVedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelserDto(
    sanityBegrunnelser: List<SanityBegrunnelse>,
    sanityEØSBegrunnelser: List<SanityEØSBegrunnelse>,
    alleBegrunnelserSkalStøtteFritekst: Boolean,
): UtvidetVedtaksperiodeMedBegrunnelserDto =
    UtvidetVedtaksperiodeMedBegrunnelserDto(
        id = this.id,
        fom = this.fom,
        tom = this.tom,
        type = this.type,
        begrunnelser =
            this.begrunnelser.map {
                it.tilVedtaksbegrunnelseDto(
                    sanityBegrunnelser = sanityBegrunnelser,
                    alleBegrunnelserSkalStøtteFritekst = alleBegrunnelserSkalStøtteFritekst,
                )
            } +
                this.eøsBegrunnelser.map {
                    it.tilVedtaksbegrunnelseDto(
                        sanityBegrunnelser = sanityEØSBegrunnelser,
                        alleBegrunnelserSkalStøtteFritekst = alleBegrunnelserSkalStøtteFritekst,
                    )
                },
        fritekster = this.fritekster,
        utbetalingsperiodeDetaljer = this.utbetalingsperiodeDetaljer,
        gyldigeBegrunnelser = this.gyldigeBegrunnelser.map { it.enumnavnTilString() },
    )
