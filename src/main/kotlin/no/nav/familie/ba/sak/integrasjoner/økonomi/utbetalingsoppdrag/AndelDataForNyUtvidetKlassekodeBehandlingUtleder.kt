package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelDataLongId
import org.springframework.stereotype.Component
import java.time.YearMonth

@Component
class AndelDataForNyUtvidetKlassekodeBehandlingUtleder(
    private val clockProvider: ClockProvider,
) {
    fun finnForrigeAndelerForNyUtvidetKlassekodeBehandling(
        forrigeTilkjentYtelse: TilkjentYtelse,
        skalBrukeNyKlassekodeForUtvidetBarnetrygd: Boolean,
    ): List<AndelDataLongId> {
        val inneværendeMåned = YearMonth.now(clockProvider.get())
        val utvidetAndeler =
            forrigeTilkjentYtelse.andelerTilkjentYtelse.filter { it.erUtvidet() }.mapNotNull {
                // Splitter andel som treffer inneværende måned og fjerner alle andeler som kommer etter.
                if (it.stønadFom <= inneværendeMåned && it.stønadTom > inneværendeMåned) {
                    it.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd).copy(tom = inneværendeMåned)
                } else if (it.stønadFom >= inneværendeMåned) {
                    null
                } else {
                    it.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd)
                }
            }
        val øvrigeAndeler = forrigeTilkjentYtelse.andelerTilkjentYtelse.filter { !it.erUtvidet() }.map { it.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd) }
        return øvrigeAndeler.plus(utvidetAndeler)
    }

    fun finnNyeAndelerForNyUtvidetKlassekodeBehandling(
        tilkjentYtelse: TilkjentYtelse,
        skalBrukeNyKlassekodeForUtvidetBarnetrygd: Boolean,
    ): List<AndelDataLongId> {
        val inneværendeMåned = YearMonth.now(clockProvider.get())
        val utvidetAndeler =
            tilkjentYtelse.andelerTilkjentYtelse.filter { it.erUtvidet() }.flatMap {
                val andelData = it.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd)
                // Splitter andeler som treffer YearMonth.now()
                if (it.stønadFom <= inneværendeMåned && it.stønadTom > inneværendeMåned) {
                    listOf(
                        // Sørger for at første AndelData etter splitt får en unik id. Dette sørger for at utbetalingsgenerator ikke kaster feil.
                        // Andelen skal ikke trigge en endring som fører til nytt kjedeelement, og vil på sett og vis bli ignorert. (Fordrer at vi finner en tilsvarende AndelData blandt forrigeAndeler)
                        andelData.copy(fom = it.stønadFom, tom = inneværendeMåned, id = it.id + tilkjentYtelse.andelerTilkjentYtelse.size),
                        // Siste AndelData etter splitt får id som samsvarer med AndelTilkjentYtelse slik at periodeId og forrigePeriodeId skal oppdateres på riktig andel.
                        andelData.copy(fom = inneværendeMåned.plusMonths(1), tom = it.stønadTom),
                    )
                } else {
                    listOf(andelData)
                }
            }

        val øvrigeAndeler = tilkjentYtelse.andelerTilkjentYtelse.filter { !it.erUtvidet() }.map { it.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd) }
        return øvrigeAndeler.plus(utvidetAndeler)
    }
}
