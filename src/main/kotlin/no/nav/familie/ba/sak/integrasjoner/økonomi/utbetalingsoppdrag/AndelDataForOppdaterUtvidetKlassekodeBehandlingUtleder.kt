package no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag

import no.nav.familie.ba.sak.common.ClockProvider
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.felles.utbetalingsgenerator.domain.AndelDataLongId
import org.springframework.stereotype.Component
import java.time.YearMonth

@Component
class AndelDataForOppdaterUtvidetKlassekodeBehandlingUtleder(
    private val clockProvider: ClockProvider,
) {
    fun finnForrigeAndelerForOppdaterUtvidetKlassekodeBehandling(
        forrigeTilkjentYtelse: TilkjentYtelse,
        skalBrukeNyKlassekodeForUtvidetBarnetrygd: Boolean,
    ): List<AndelDataLongId> {
        val inneværendeMåned = YearMonth.now(clockProvider.get())
        val (utvidetAndelerTilkjentYtelse, øvrigeAndelerTilkjentYtelse) = forrigeTilkjentYtelse.andelerTilkjentYtelse.partition { it.erUtvidet() }
        val utvidetAndeler =
            utvidetAndelerTilkjentYtelse.mapNotNull {
                // Splitter andel som treffer inneværende måned og fjerner alle andeler som kommer etter.
                if (it.stønadFom <= inneværendeMåned && it.stønadTom > inneværendeMåned) {
                    it.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd).copy(tom = inneværendeMåned)
                } else if (it.stønadFom >= inneværendeMåned) {
                    null
                } else {
                    it.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd)
                }
            }
        val øvrigeAndeler = øvrigeAndelerTilkjentYtelse.map { it.tilAndelDataLongId(skalBrukeNyKlassekodeForUtvidetBarnetrygd) }
        return øvrigeAndeler.plus(utvidetAndeler)
    }
}
