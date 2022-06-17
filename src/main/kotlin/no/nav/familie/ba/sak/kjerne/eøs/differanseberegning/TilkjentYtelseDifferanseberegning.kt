package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.tilKronerPerValutaenhet
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.tilMånedligValutabeløp
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.times
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed

/**
 * ADVARSEL: Muterer TilkjentYtelse
 * Denne BURDE gjøres ikke-muterbar og returnere en ny instans av TilkjentYtelse
 * Muteringen skyldes at TilkjentYtelse er under JPA-kontekst og ikke "tåler" copy(andelerTilkjentYtelse = ...)
 * Starten på én løsning er at EndretUtebetalingPeriode kobles løs fra AndelTilkjentYtelse og kobles rett på behandlingen
 */
fun beregnDifferanse(
    andelerTilkjentYtelse: Collection<AndelTilkjentYtelse>,
    utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>,
    valutakurser: Collection<Valutakurs>
): List<AndelTilkjentYtelse> {

    val utenlandskePeriodebeløpTidslinjer = utenlandskePeriodebeløp.tilSeparateTidslinjerForBarna()
    val valutakursTidslinjer = valutakurser.tilSeparateTidslinjerForBarna()
    val andelTilkjentYtelseTidslinjer = andelerTilkjentYtelse.tilSeparateTidslinjerForBarna()

    val alleBarna: Set<Aktør> =
        utenlandskePeriodebeløpTidslinjer.keys + valutakursTidslinjer.keys + andelTilkjentYtelseTidslinjer.keys

    val barnasDifferanseberegnetAndelTilkjentYtelseTidslinjer = alleBarna.map { aktør ->
        val utenlandskePeriodebeløpTidslinje = utenlandskePeriodebeløpTidslinjer.getOrDefault(aktør, TomTidslinje())
        val valutakursTidslinje = valutakursTidslinjer.getOrDefault(aktør, TomTidslinje())
        val andelTilkjentYtelseTidslinje = andelTilkjentYtelseTidslinjer.getOrDefault(aktør, TomTidslinje())

        val utenlandskePeriodebeløpINorskeKroner = utenlandskePeriodebeløpTidslinje.kombinerMed(valutakursTidslinje) { upb, valutakurs -> upb.tilMånedligValutabeløp() * valutakurs.tilKronerPerValutaenhet() }

        andelTilkjentYtelseTidslinje.kombinerMed(utenlandskePeriodebeløpINorskeKroner) { aty, beløp ->
            differanseBeregnUtbetalingsbeløp(aty, beløp)
        }
    }

    val barnasAndeler = barnasDifferanseberegnetAndelTilkjentYtelseTidslinjer.tilAndelerTilkjentYtelse()
    val søkersAndeler = andelerTilkjentYtelse.søkersAndeler()

    validarSøkersYtelserMotEventueltNegativeAndelerForBarna(søkersAndeler, barnasAndeler)

    // Muterer tilkjentYtelse, lager IKKE ny instans
    return søkersAndeler + barnasAndeler
}

private fun validarSøkersYtelserMotEventueltNegativeAndelerForBarna(
    søkersAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    barnasAndelerTilkjentYtelse: List<AndelTilkjentYtelse>
) {
    val barnasSumNegativeDifferansebeløp = barnasAndelerTilkjentYtelse
        .map { minOf(it.differanseberegnetPeriodebeløp ?: 0, 0) }
        .sum()

    val søkersSumUtbetalingsbeløp = søkersAndelerTilkjentYtelse
        .map { it.kalkulertUtbetalingsbeløp }
        .sum()

    if (barnasSumNegativeDifferansebeløp < 0 && søkersSumUtbetalingsbeløp > 0)
        TODO(
            "Søker har småbarnstillegg og/elleer utvidet barnetrygd, " +
                "samtidig som ett eller flere barn har endt med negative utbetalingsbeløp etter differanseberegning. " +
                "Det er ikke støttet ennå"
        )
}
