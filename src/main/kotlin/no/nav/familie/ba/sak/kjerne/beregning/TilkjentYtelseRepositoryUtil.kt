package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.AndelTilkjentYtelsePraktiskLikhet.erIPraksisLik
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.AndelTilkjentYtelsePraktiskLikhet.inneholderIPraksis

/**
 * En litt risikabel funksjon, som benytter "funksjonell likhet" for å sjekke etter endringer på andel tilkjent ytelse
 */
fun TilkjentYtelseRepository.oppdaterTilkjentYtelse(
    tilkjentYtelse: TilkjentYtelse,
    oppdaterteAndeler: Collection<AndelTilkjentYtelse>,
): TilkjentYtelse {
    if (tilkjentYtelse.andelerTilkjentYtelse.erIPraksisLik(oppdaterteAndeler)) {
        return tilkjentYtelse
    }

    // Her er det viktig å beholde de originale andelene, som styres av JPA og har alt av innhold
    val skalBeholdes =
        tilkjentYtelse.andelerTilkjentYtelse
            .filter { oppdaterteAndeler.inneholderIPraksis(it) }

    val skalLeggesTil =
        oppdaterteAndeler
            .filter { !tilkjentYtelse.andelerTilkjentYtelse.inneholderIPraksis(it) }

    // Forsikring: Sjekk at det ikke oppstår eller forsvinner andeler når de sjekkes for likhet
    if (oppdaterteAndeler.size != (skalBeholdes.size + skalLeggesTil.size)) {
        throw Feil("Avvik mellom antall innsendte andeler og kalkulerte endringer")
    }

    tilkjentYtelse.andelerTilkjentYtelse.clear()
    tilkjentYtelse.andelerTilkjentYtelse.addAll(skalBeholdes + skalLeggesTil)

    return this.saveAndFlush(tilkjentYtelse)
}
