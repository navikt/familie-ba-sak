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

    val forskjell: AndelOgOppdragForskjell = hentForskjellIAndelerOgUtbetalingsoppdrag(oppdragsperioder, andeler)

    when (forskjell) {
        AndelOgOppdragForskjell.TOMT_OPPDRAG_OG_ANDELER_MED_UTBETALIGNG -> secureLogger.info(
            "Fagsak $fagsakId har andeler med utbetaling, men har ikke sendt noe oppdragsperioder til økonomi"
        )

        AndelOgOppdragForskjell.FORSKJELLIG_AVSLUTTNING_DATO -> secureLogger.info(
            "Fagsak $fagsakId sine andeler har annen avsluttnignsdato enn oppdraget som er sendt til økonomi"
        )

        AndelOgOppdragForskjell.OPPDRAGSPERIODE_UTEN_TILSVARENDE_ANDEL -> secureLogger.info(
            "Fagsak $fagsakId har sendt utbetalingsoppdrag til økonomi som ikke har tilsvarende andel tilkjent ytelse"
        )
        AndelOgOppdragForskjell.INGEN_FORSKJELL -> Unit
    }

    return forskjell != AndelOgOppdragForskjell.INGEN_FORSKJELL
}

private fun hentForskjellIAndelerOgUtbetalingsoppdrag(
    oppdragsperioder: List<Utbetalingsperiode>,
    andeler: List<AndelTilkjentYtelse>
) = when {
    erOppdragTomtOgAndelerMedUtbetaling(oppdragsperioder, andeler) ->
        AndelOgOppdragForskjell.TOMT_OPPDRAG_OG_ANDELER_MED_UTBETALIGNG

    erForskjelligTomDato(oppdragsperioder, andeler) ->
        AndelOgOppdragForskjell.FORSKJELLIG_AVSLUTTNING_DATO

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

private fun erForskjelligTomDato(
    oppdragsperioder: List<Utbetalingsperiode>,
    andeler: List<AndelTilkjentYtelse>
) = oppdragsperioder.maxOf { it.vedtakdatoTom.toYearMonth() } != andeler.maxOf { it.stønadTom }

private fun erOppdragTomtOgAndelerMedUtbetaling(
    oppdragsperioder: List<Utbetalingsperiode>,
    andeler: List<AndelTilkjentYtelse>
) = oppdragsperioder.isEmpty() && andeler.sumOf { it.kalkulertUtbetalingsbeløp } != 0

enum class AndelOgOppdragForskjell {
    TOMT_OPPDRAG_OG_ANDELER_MED_UTBETALIGNG,
    FORSKJELLIG_AVSLUTTNING_DATO,
    OPPDRAGSPERIODE_UTEN_TILSVARENDE_ANDEL,
    INGEN_FORSKJELL
}
