package no.nav.familie.ba.sak.integrasjoner.økonomi.InternKonsistendsavstemming

import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import java.math.BigDecimal

fun erForskjellMellomAndelerOgOppdrag(
    andeler: List<AndelTilkjentYtelse>,
    utbetalingsoppdrag: Utbetalingsoppdrag?,
    fagsakId: Long
): Boolean {
    val utbetalingsperioder =
        utbetalingsoppdrag?.utbetalingsperiode
            ?.filter { it.opphør == null }
            ?: emptyList()

    val forskjellMellomAndeleneOgUtbetalingsoppdraget =
        hentForskjellIAndelerOgUtbetalingsoppdrag(utbetalingsperioder, andeler)

    when (forskjellMellomAndeleneOgUtbetalingsoppdraget) {
        is UTBETALINGSPERIODER_UTEN_TILSVARENDE_ANDEL -> secureLogger.info(
            "Fagsak $fagsakId har sendt utbetalingsperiode(r) til økonomi som ikke har tilsvarende andel tilkjent ytelse." +
                "\nDet er differanse i perioden(e) ${forskjellMellomAndeleneOgUtbetalingsoppdraget.utbetalingsperioder.tilTidStrenger()}." +
                "\n\nSiste utbetalingsoppdrag som er sendt til familie-øknonomi på fagsaken er:" +
                "\n$utbetalingsoppdrag"
        )

        is INGEN_FORSKJELL -> Unit
    }

    return forskjellMellomAndeleneOgUtbetalingsoppdraget !is INGEN_FORSKJELL
}

private fun hentForskjellIAndelerOgUtbetalingsoppdrag(
    utbetalingsperioder: List<Utbetalingsperiode>,
    andeler: List<AndelTilkjentYtelse>
): AndelOgOppdragForskjell {
    val utbetalingsperioderUtenTilsvarendeAndel = utbetalingsperioder.filter {
        it.erIngenPersonerMedTilsvarendeAndelITidsrommet(andeler)
    }

    return if (utbetalingsperioderUtenTilsvarendeAndel.isEmpty()) {
        INGEN_FORSKJELL
    } else {
        UTBETALINGSPERIODER_UTEN_TILSVARENDE_ANDEL(utbetalingsperioderUtenTilsvarendeAndel)
    }
}

private fun Utbetalingsperiode.erIngenPersonerMedTilsvarendeAndelITidsrommet(
    andeler: List<AndelTilkjentYtelse>
): Boolean {
    val andelsTidslinjerPerPersonOgYtelsetype = andeler
        .groupBy { Pair(it.aktør, it.type) }
        .map { (_, andeler) -> andeler.tilBeløpstidslinje() }

    return andelsTidslinjerPerPersonOgYtelsetype.all {
        !this.harTilsvarendeAndelerForPersonOgYtelsetype(it)
    }
}

private fun Utbetalingsperiode.harTilsvarendeAndelerForPersonOgYtelsetype(
    andelerTidslinjeForEnPersonOgYtelsetype: Tidslinje<BigDecimal, Måned>
): Boolean {
    val erAndelLikUtbetalingTidslinje = this.tilBeløpstidslinje()
        .kombinerMed(andelerTidslinjeForEnPersonOgYtelsetype) { utbetalingsperiode, andel ->
            utbetalingsperiode?.let { utbetalingsperiode == andel }
        }

    return erAndelLikUtbetalingTidslinje.perioder().all { it.innhold != false }
}

private fun Utbetalingsperiode.tilBeløpstidslinje(): Tidslinje<BigDecimal, Måned> = tidslinje {
    listOf(
        Periode(
            fraOgMed = this.vedtakdatoFom.tilMånedTidspunkt(),
            tilOgMed = this.vedtakdatoTom.tilMånedTidspunkt(),
            innhold = this.sats
        )
    )
}

private fun List<AndelTilkjentYtelse>.tilBeløpstidslinje(): Tidslinje<BigDecimal, Måned> = tidslinje {
    this.map {
        Periode(
            fraOgMed = it.stønadFom.tilTidspunkt(),
            tilOgMed = it.stønadTom.tilTidspunkt(),
            innhold = it.kalkulertUtbetalingsbeløp.toBigDecimal()
        )
    }
}

private fun List<Utbetalingsperiode>.tilTidStrenger() =
    Utils.slåSammen(this.map { "${it.vedtakdatoFom.toYearMonth()} til ${it.vedtakdatoTom.toYearMonth()}" })

private sealed interface AndelOgOppdragForskjell

private data class UTBETALINGSPERIODER_UTEN_TILSVARENDE_ANDEL(val utbetalingsperioder: List<Utbetalingsperiode>) :
    AndelOgOppdragForskjell

private object INGEN_FORSKJELL : AndelOgOppdragForskjell
