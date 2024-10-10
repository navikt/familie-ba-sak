package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.felles.utbetalingsgenerator.domain.IdentOgType

object BeregningTestUtil {
    fun sisteAndelPerIdentNy(tilkjenteYtelser: List<TilkjentYtelse>): Map<IdentOgType, AndelTilkjentYtelse> =
        tilkjenteYtelser
            .flatMap { it.andelerTilkjentYtelse }
            .groupBy { IdentOgType(it.aktør.aktivFødselsnummer(), it.type.tilYtelseType()) }
            .mapValues { it.value.maxBy { it.periodeOffset ?: 0 } }
}
