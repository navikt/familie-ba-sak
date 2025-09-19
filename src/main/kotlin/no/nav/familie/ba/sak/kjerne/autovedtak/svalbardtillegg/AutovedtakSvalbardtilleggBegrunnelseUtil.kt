package no.nav.familie.ba.sak.kjerne.autovedtak.svalbardstillegg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.tilAndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.tilTidslinje
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import java.time.YearMonth

fun finnInnvilgedeOgReduserteSvalbardtilleggPerioder(
    forrigeAndeler: List<AndelTilkjentYtelse>,
    nåværendeAndeler: List<AndelTilkjentYtelse>,
): Pair<Set<YearMonth>, Set<YearMonth>> {
    val forrigeSvalbardtilleggAndeler = forrigeAndeler.filter { it.erSvalbardtillegg() }
    val nåværendeSvalbardtilleggAndeler = nåværendeAndeler.filter { it.erSvalbardtillegg() }

    val relevanteBarn =
        (forrigeSvalbardtilleggAndeler + nåværendeSvalbardtilleggAndeler)
            .map { it.aktør }
            .toSet()

    val innvilgedeOgReduserteSvalbardtilleggPerioder =
        relevanteBarn.fold<Aktør, Pair<Set<YearMonth>, Set<YearMonth>>>(emptySet<YearMonth>() to emptySet()) { (nyePerioder, reduksjonsPerioder), barn ->
            val forrigeSvalbardtilleggsAndelerTidslinje = forrigeSvalbardtilleggAndeler.filter { it.aktør == barn }.tilTidslinje()
            val nåværendeSvalbardtilleggAndelerTidslinje = nåværendeSvalbardtilleggAndeler.filter { it.aktør == barn }.tilTidslinje()

            val nyeAndeler = forrigeSvalbardtilleggsAndelerTidslinje.kombinerMed(nåværendeSvalbardtilleggAndelerTidslinje) { gammel, ny -> ny.takeIf { gammel == null } }
            val fjernetAndeler = forrigeSvalbardtilleggsAndelerTidslinje.kombinerMed(nåværendeSvalbardtilleggAndelerTidslinje) { gammel, ny -> gammel.takeIf { ny == null } }

            (nyePerioder + nyeAndeler.tilAndelTilkjentYtelse().map { it.stønadFom } to reduksjonsPerioder + fjernetAndeler.tilAndelTilkjentYtelse().map { it.stønadFom })
        }

    return innvilgedeOgReduserteSvalbardtilleggPerioder
}

internal fun leggTilBegrunnelseIVedtaksperiode(
    vedtaksperiodeStartDato: YearMonth,
    standardbegrunnelse: Standardbegrunnelse,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
) {
    val vedtaksperiode =
        vedtaksperioder.find {
            (it.type == Vedtaksperiodetype.UTBETALING || it.type == Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING) &&
                it.fom?.toYearMonth() == vedtaksperiodeStartDato
        } ?: run {
            secureLogger.info(
                "Finner ikke aktuell periode å begrunne ved autovedtak svalbardtillegg. " +
                    "Periode: $vedtaksperiodeStartDato. " +
                    "Perioder: ${vedtaksperioder.map { "Periode(type=${it.type}, fom=${it.fom}, tom=${it.tom})" }}",
            )
            throw Feil(
                "Finner ikke aktuell periode å begrunne ved autovedtak svalbardtillegg. Se securelogger for detaljer.",
            )
        }

    vedtaksperiode.settBegrunnelser(
        (
            vedtaksperiode.begrunnelser +
                Vedtaksbegrunnelse(
                    vedtaksperiodeMedBegrunnelser = vedtaksperiode,
                    standardbegrunnelse = standardbegrunnelse,
                )
        ).toList(),
    )
}
