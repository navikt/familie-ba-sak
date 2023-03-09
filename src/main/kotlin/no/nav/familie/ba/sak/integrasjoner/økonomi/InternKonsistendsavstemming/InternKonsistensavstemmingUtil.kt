package no.nav.familie.ba.sak.integrasjoner.økonomi.InternKonsistendsavstemming

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode

fun erForskjellMellomAndelerOgOppdrag(
    andeler: List<AndelTilkjentYtelse>,
    utbetalingsoppdrag: Utbetalingsoppdrag?,
    fagsakId: Long
): Boolean {
    val oppdragsperioder =
        utbetalingsoppdrag?.utbetalingsperiode
            ?.filter { it.opphør == null }
            ?: emptyList()

    val forskjellMellomAndeleneOgUtbetalingsoppdraget: AndelOgOppdragForskjell = hentForskjellIAndelerOgUtbetalingsoppdrag(oppdragsperioder, andeler)

    when (forskjellMellomAndeleneOgUtbetalingsoppdraget) {
        AndelOgOppdragForskjell.OPPDRAGSPERIODE_UTEN_TILSVARENDE_ANDEL -> secureLogger.info(
            "Fagsak $fagsakId har sendt utbetalingsperiode(r) til økonomi som ikke har tilsvarende andel tilkjent ytelse" +
                "Siste utbetalingsoppdrag som er sendt til familie-øknonomi på fagsaken er:" +
                "\n\n $utbetalingsoppdrag"
        )
        AndelOgOppdragForskjell.INGEN_FORSKJELL -> Unit
    }

    return forskjellMellomAndeleneOgUtbetalingsoppdraget != AndelOgOppdragForskjell.INGEN_FORSKJELL
}

private fun hentForskjellIAndelerOgUtbetalingsoppdrag(
    oppdragsperioder: List<Utbetalingsperiode>,
    andeler: List<AndelTilkjentYtelse>
) = when {
    erOppdragsperiodeUtenTilsvarendeAndel(oppdragsperioder, andeler) ->
        AndelOgOppdragForskjell.OPPDRAGSPERIODE_UTEN_TILSVARENDE_ANDEL

    else -> AndelOgOppdragForskjell.INGEN_FORSKJELL
}

private fun erOppdragsperiodeUtenTilsvarendeAndel(
    oppdragsperioder: List<Utbetalingsperiode>,
    andeler: List<AndelTilkjentYtelse>
) = !oppdragsperioder.all { oppdragsperiode ->
    andeler.any {
        it.kalkulertUtbetalingsbeløp.toBigDecimal() == oppdragsperiode.sats &&
            it.stønadFom == oppdragsperiode.vedtakdatoFom.toYearMonth() &&
            it.stønadTom == oppdragsperiode.vedtakdatoTom.toYearMonth()
    }
}

enum class AndelOgOppdragForskjell {
    OPPDRAGSPERIODE_UTEN_TILSVARENDE_ANDEL,
    INGEN_FORSKJELL
}
