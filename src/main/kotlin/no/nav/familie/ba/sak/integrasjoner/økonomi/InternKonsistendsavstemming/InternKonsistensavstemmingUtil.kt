package no.nav.familie.ba.sak.integrasjoner.økonomi.InternKonsistendsavstemming

import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilMånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.MånedTidspunkt.Companion.tilTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilYearMonth
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
        is OPPDRAGSPERIODER_UTEN_TILSVARENDE_ANDEL -> secureLogger.info(
            "Fagsak $fagsakId har sendt utbetalingsperiode(r) til økonomi som ikke har tilsvarende andel tilkjent ytelse." +
                "\nDet er differanse i periodene ${forskjellMellomAndeleneOgUtbetalingsoppdraget.perioderMedForskjell.tilTidStrenger()}." +
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
    val erBeløpLiktTidslinje = utbetalingsperioder.tilBeløpstidslinje()
        .kombinerMed(andeler.tilBeløpstidslinje()) { utbetaling, andel ->
            utbetaling == andel
        }

    val perioderMedForskjell = erBeløpLiktTidslinje.perioder().filter { it.innhold == false }

    return if (perioderMedForskjell.isEmpty()) {
        INGEN_FORSKJELL
    } else {
        OPPDRAGSPERIODER_UTEN_TILSVARENDE_ANDEL(perioderMedForskjell)
    }
}

private fun List<Utbetalingsperiode>.tilBeløpstidslinje(): Tidslinje<BigDecimal, Måned> = tidslinje {
    this.map {
        Periode(
            fraOgMed = it.vedtakdatoFom.tilMånedTidspunkt(),
            tilOgMed = it.vedtakdatoTom.tilMånedTidspunkt(),
            innhold = it.sats
        )
    }
}

@JvmName("atyListeTilBeløpstidslinje")
private fun List<AndelTilkjentYtelse>.tilBeløpstidslinje(): Tidslinje<BigDecimal, Måned> = tidslinje {
    this.map {
        Periode(
            fraOgMed = it.stønadFom.tilTidspunkt(),
            tilOgMed = it.stønadTom.tilTidspunkt(),
            innhold = it.kalkulertUtbetalingsbeløp.toBigDecimal()
        )
    }
}

private fun List<Periode<*, Måned>>.tilTidStrenger() =
    Utils.slåSammen(this.map { "${it.fraOgMed.tilYearMonth()} - ${it.tilOgMed.tilYearMonth()}" })

private sealed interface AndelOgOppdragForskjell

private data class OPPDRAGSPERIODER_UTEN_TILSVARENDE_ANDEL(val perioderMedForskjell: List<Periode<Boolean, Måned>>) :
    AndelOgOppdragForskjell

private object INGEN_FORSKJELL : AndelOgOppdragForskjell
