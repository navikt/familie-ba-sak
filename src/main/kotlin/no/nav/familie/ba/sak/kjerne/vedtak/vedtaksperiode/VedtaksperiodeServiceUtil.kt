package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.lagVertikaleSegmenter
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser

fun hentVedtaksperioderMedBegrunnelserForEndredeUtbetalingsperioder(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    vedtak: Vedtak
) = andelerTilkjentYtelse.filter { it.endretUtbetalingAndeler.isNotEmpty() }.groupBy { it.prosent }
    .flatMap { (_, andeler) ->
        andeler.lagVertikaleSegmenter()
            .map { (segmenter, andelerForSegment) ->
                VedtaksperiodeMedBegrunnelser(
                    fom = segmenter.fom,
                    tom = segmenter.tom,
                    vedtak = vedtak,
                    type = Vedtaksperiodetype.ENDRET_UTBETALING
                ).also { vedtaksperiodeMedBegrunnelse ->
                    val endretUtbetalingAndeler = andelerForSegment.flatMap { it.endretUtbetalingAndeler }
                    vedtaksperiodeMedBegrunnelse.begrunnelser.addAll(
                        endretUtbetalingAndeler
                            .flatMap { it.vedtakBegrunnelseSpesifikasjoner }.toSet()
                            .map { vedtakBegrunnelseSpesifikasjon ->
                                Vedtaksbegrunnelse(
                                    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelse,
                                    vedtakBegrunnelseSpesifikasjon = vedtakBegrunnelseSpesifikasjon,
                                    personIdenter = endretUtbetalingAndeler.filter {
                                        it.harVedtakBegrunnelseSpesifikasjon(vedtakBegrunnelseSpesifikasjon)
                                    }.mapNotNull { it.person?.personIdent?.ident }
                                )
                            }
                    )
                }
            }
    }

fun hentVedtaksperioderMedBegrunnelserForUtbetalingsperioder(
    andelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    vedtak: Vedtak
) = andelerTilkjentYtelse.filter { it.endretUtbetalingAndeler.isEmpty() }.lagVertikaleSegmenter()
    .map { (segmenter, _) ->
        VedtaksperiodeMedBegrunnelser(
            fom = segmenter.fom,
            tom = segmenter.tom,
            vedtak = vedtak,
            type = Vedtaksperiodetype.UTBETALING
        )
    }
