package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse

fun TilkjentYtelse.medAndeler(andeler: Iterable<AndelTilkjentYtelse>): TilkjentYtelse {
    this.andelerTilkjentYtelse.clear()
    this.andelerTilkjentYtelse.addAll(andeler)
    return this
}

fun TilkjentYtelse.søkersAndeler() = this.andelerTilkjentYtelse.filter { it.erSøkersAndel() }
