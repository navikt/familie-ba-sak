package no.nav.familie.ba.sak.kjerne.autovedtak.søknad

import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.tilTidslinjerPerAktørOgType
import no.nav.familie.ba.sak.kjerne.beregning.tilAndelerTilkjentYtelse
import no.nav.familie.tidslinje.utvidelser.outerJoin
import java.time.YearMonth

fun finnNyeAndeler(
    forrigeAndeler: List<AndelTilkjentYtelse>,
    nåværendeAndeler: List<AndelTilkjentYtelse>,
): List<AndelTilkjentYtelse> =
    nåværendeAndeler
        .tilTidslinjerPerAktørOgType()
        .outerJoin(forrigeAndeler.tilTidslinjerPerAktørOgType()) { nåværendeAndel, forrigeAndel -> nåværendeAndel.takeIf { forrigeAndel == null } }
        .values
        .tilAndelerTilkjentYtelse()

fun List<AndelTilkjentYtelse>.finnFørsteMånederMedUtbetaling(ytelseType: YtelseType): Set<YearMonth> =
    andelerMedUtbetalingPerBarn(ytelseType)
        .flatMap { andelerForBarn -> andelerForBarn.filterIndexed { indeks, andel -> indeks == 0 || !andelerForBarn[indeks - 1].slutterRettFør(andel) } }
        .map { it.stønadFom }
        .toSet()

fun List<AndelTilkjentYtelse>.finnMånederMedSatsøkning(): Set<YearMonth> =
    andelerMedUtbetalingPerBarn(YtelseType.ORDINÆR_BARNETRYGD)
        .flatMap { andelerForBarn -> andelerForBarn.zipWithNext().filter { (forrige, neste) -> forrige.slutterRettFør(neste) && neste.sats > forrige.sats } }
        .map { (_, neste) -> neste.stønadFom }
        .toSet()

fun List<AndelTilkjentYtelse>.harAndelUtenUtbetalingI(måned: YearMonth): Boolean = any { it.kalkulertUtbetalingsbeløp == 0 && måned in it.stønadFom..it.stønadTom }

private fun List<AndelTilkjentYtelse>.andelerMedUtbetalingPerBarn(ytelseType: YtelseType): List<List<AndelTilkjentYtelse>> =
    filter { it.type == ytelseType && it.kalkulertUtbetalingsbeløp > 0 }
        .groupBy { it.aktør }
        .values
        .map { andelerForBarn -> andelerForBarn.sortedBy { it.stønadFom } }

private fun AndelTilkjentYtelse.slutterRettFør(neste: AndelTilkjentYtelse): Boolean = stønadTom.nesteMåned() == neste.stønadFom
