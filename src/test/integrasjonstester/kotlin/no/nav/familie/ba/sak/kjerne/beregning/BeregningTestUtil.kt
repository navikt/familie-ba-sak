package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForSimuleringFactory
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForUtbetalingsoppdrag
import no.nav.familie.ba.sak.integrasjoner.økonomi.IdentOgYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse

object BeregningTestUtil {

    /**
     * Denne erstatter det som [no.nav.familie.ba.sak.kjerne.beregning.BeregningService.hentSisteAndelPerIdent] gjør
     * Pga at det ikke er den samme implementasjonen burde bruket av denne minimeres
     */
    fun sisteAndelPerIdent(tilkjenteYtelser: List<TilkjentYtelse>): Map<IdentOgYtelse, AndelTilkjentYtelseForUtbetalingsoppdrag> {
        val andeler = tilkjenteYtelser.flatMap { it.andelerTilkjentYtelse }
        return sisteAndelPerIdent(andeler)
    }

    @JvmName("sisteAndelTilkjentYtelsePerIdent")
    fun sisteAndelPerIdent(andeler: List<AndelTilkjentYtelse>): Map<IdentOgYtelse, AndelTilkjentYtelseForUtbetalingsoppdrag> {
        val factory = AndelTilkjentYtelseForSimuleringFactory()
        return sisteAndelPerIdent(factory.pakkInnForUtbetaling(andeler))
    }

    @JvmName("sisteAndelPerIdentAndelUtbetalingsoppdrag")
    fun sisteAndelPerIdent(andeler: List<AndelTilkjentYtelseForUtbetalingsoppdrag>): Map<IdentOgYtelse, AndelTilkjentYtelseForUtbetalingsoppdrag> {
        return andeler
            .groupBy { IdentOgYtelse(it.aktør.aktivFødselsnummer(), it.type) }
            .mapValues { it.value.maxBy { it.periodeOffset!! } }
    }
}
