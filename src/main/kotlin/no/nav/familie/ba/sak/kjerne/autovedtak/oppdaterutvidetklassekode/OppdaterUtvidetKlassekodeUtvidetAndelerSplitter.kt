package no.nav.familie.ba.sak.kjerne.autovedtak.oppdaterutvidetklassekode

import no.nav.familie.ba.sak.common.inneværendeMåned
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse

object OppdaterUtvidetKlassekodeUtvidetAndelerSplitter {
    fun splittUtvidetAndelerIInneværendeMåned(andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>): List<AndelTilkjentYtelse> {
        val inneværendeMåned = inneværendeMåned()
        return andelerTilkjentYtelse.flatMap {
            if (it.erUtvidet() && it.stønadFom <= inneværendeMåned && it.stønadTom > inneværendeMåned) {
                listOf(
                    it.copy(stønadFom = it.stønadFom, stønadTom = inneværendeMåned),
                    it.copy(stønadFom = inneværendeMåned.plusMonths(1), stønadTom = it.stønadTom),
                )
            } else {
                listOf(it)
            }
        }
    }
}
