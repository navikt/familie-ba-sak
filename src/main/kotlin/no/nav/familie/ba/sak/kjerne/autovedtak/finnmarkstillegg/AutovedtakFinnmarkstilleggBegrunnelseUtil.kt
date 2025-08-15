package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.tilAndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.tilTidslinje
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.tidslinje.utvidelser.kombinerMed
import java.time.YearMonth

fun finnInnvilgedeOgReduserteFinnmarkstilleggPerioder(
    forrigeAndeler: List<AndelTilkjentYtelse>,
    nåværendeAndeler: List<AndelTilkjentYtelse>,
): Pair<Set<YearMonth>, Set<YearMonth>> {
    val forrigeFinnmarkstilleggAndeler = forrigeAndeler.filter { it.erFinnmarkstillegg() }
    val nåværendeFinnmarkstilleggAndeler = nåværendeAndeler.filter { it.erFinnmarkstillegg() }

    val relevanteBarn =
        (forrigeFinnmarkstilleggAndeler + nåværendeFinnmarkstilleggAndeler)
            .map { it.aktør }
            .toSet()

    val innvilgedeOgReduserteFinnmarkstilleggPerioder =
        relevanteBarn.fold<Aktør, Pair<Set<YearMonth>, Set<YearMonth>>>(emptySet<YearMonth>() to emptySet()) { (nyePerioder, reduksjonsPerioder), barn ->
            val forrigeFinnmarkstilleggsAndelerTidslinje = forrigeFinnmarkstilleggAndeler.filter { it.aktør == barn }.tilTidslinje()
            val nåværendeFinnmarkstilleggAndelerTidslinje = nåværendeFinnmarkstilleggAndeler.filter { it.aktør == barn }.tilTidslinje()

            val nyeAndeler = forrigeFinnmarkstilleggsAndelerTidslinje.kombinerMed(nåværendeFinnmarkstilleggAndelerTidslinje) { gammel, ny -> ny.takeIf { gammel == null } }
            val fjernetAndeler = forrigeFinnmarkstilleggsAndelerTidslinje.kombinerMed(nåværendeFinnmarkstilleggAndelerTidslinje) { gammel, ny -> gammel.takeIf { ny == null } }

            (nyePerioder + nyeAndeler.tilAndelTilkjentYtelse().map { it.stønadFom } to reduksjonsPerioder + fjernetAndeler.tilAndelTilkjentYtelse().map { it.stønadFom })
        }

    return innvilgedeOgReduserteFinnmarkstilleggPerioder
}

internal fun leggTilBegrunnelseIVedtaksperiode(
    vedtaksperiodeStartDato: YearMonth,
    standardbegrunnelse: Standardbegrunnelse,
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
) {
    val vedtaksperiode =
        vedtaksperioder.find {
            it.type == Vedtaksperiodetype.UTBETALING &&
                it.fom?.toYearMonth() == vedtaksperiodeStartDato
        } ?: run {
            secureLogger.info(
                "Finner ikke aktuell periode å begrunne ved autovedtak finnmarkstillegg. " +
                    "Periode: $vedtaksperiodeStartDato. " +
                    "Perioder: ${vedtaksperioder.map { "Periode(type=${it.type}, fom=${it.fom}, tom=${it.tom})" }}",
            )
            throw Feil(
                "Finner ikke aktuell periode å begrunne ved autovedtak finnmarkstillegg. Se securelogger for detaljer.",
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
