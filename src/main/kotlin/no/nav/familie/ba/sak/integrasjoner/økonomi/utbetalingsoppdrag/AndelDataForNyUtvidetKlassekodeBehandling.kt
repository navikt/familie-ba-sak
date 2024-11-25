package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelDataLongId
import java.time.YearMonth

fun finnForrigeAndelerForNyUtvidetKlassekodeBehandling(
    forrigeTilkjentYtelse: TilkjentYtelse?,
    skalBrukeNyKlassekodeForUtvidetBarnetrygd: Boolean,
): List<AndelDataLongId> =
    if (forrigeTilkjentYtelse == null) {
        emptyList()
    } else {
        val utvidetAndeler =
            // Fjerner alle utvidet andeler som kommer etter YearMonth.now()
            forrigeTilkjentYtelse.andelerTilkjentYtelse.filter { it.erUtvidet() }.mapNotNull {
                if (it.stønadFom <= YearMonth.now() && it.stønadTom > YearMonth.now()) {
                    it.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd).copy(tom = YearMonth.now())
                } else if (it.stønadFom > YearMonth.now()) {
                    null
                } else {
                    it.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd)
                }
            }
        val øvrigeAndeler = forrigeTilkjentYtelse.andelerTilkjentYtelse.filter { !it.erUtvidet() }.map { it.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd) }
        øvrigeAndeler.plus(utvidetAndeler)
    }

fun finnNyeAndelerForNyUtvidetKlassekodeBehandling(
    tilkjentYtelse: TilkjentYtelse,
    skalBrukeNyKlassekodeForUtvidetBarnetrygd: Boolean,
): List<AndelDataLongId> {
    val utvidetAndeler =
        // Splitter andeler som treffer YearMonth.now()
        tilkjentYtelse.andelerTilkjentYtelse.filter { it.erUtvidet() }.flatMap {
            val andelData = it.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd)
            if (it.stønadFom <= YearMonth.now() && it.stønadTom > YearMonth.now()) {
                listOf(
                    andelData.copy(fom = it.stønadFom, tom = YearMonth.now(), id = it.id + tilkjentYtelse.andelerTilkjentYtelse.size),
                    andelData.copy(fom = YearMonth.now().plusMonths(1), tom = it.stønadTom),
                )
            } else {
                listOf(andelData)
            }
        }

    val øvrigeAndeler = tilkjentYtelse.andelerTilkjentYtelse.filter { !it.erUtvidet() }.map { it.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd) }
    return øvrigeAndeler.plus(utvidetAndeler)
}
