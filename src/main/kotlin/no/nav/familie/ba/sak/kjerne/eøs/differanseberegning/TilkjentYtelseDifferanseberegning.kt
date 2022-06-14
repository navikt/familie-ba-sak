package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.tilKronerPerValutaenhet
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.tilMånedligValutabeløp
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.domene.times
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TomTidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.tidspunktKombinerMed

fun beregnDifferanse(
    tilkjentYtelse: TilkjentYtelse,
    utenlandskePeriodebeløp: Collection<UtenlandskPeriodebeløp>,
    valutakurser: Collection<Valutakurs>
): TilkjentYtelse {

    if (utenlandskePeriodebeløp.isEmpty() || valutakurser.isEmpty())
        return tilkjentYtelse

    val utenlandskePeriodebeløpTidslinjer = utenlandskePeriodebeløp.tilSeparateTidslinjerForBarna()
    val valutakursTidslinjer = valutakurser.tilSeparateTidslinjerForBarna()
    val andelTilkjentYtelseTidslinjer = tilkjentYtelse.tilSeparateTidslinjerForBarna()

    val alleBarna: Set<Aktør> =
        utenlandskePeriodebeløpTidslinjer.keys + valutakursTidslinjer.keys + andelTilkjentYtelseTidslinjer.keys

    val barnasDifferanseberegnetAndelTilkjentYtelseTidslinjer = alleBarna.map { aktør ->
        val utenlandskePeriodebeløpTidslinje = utenlandskePeriodebeløpTidslinjer.getOrDefault(aktør, TomTidslinje())
        val valutakursTidslinje = valutakursTidslinjer.getOrDefault(aktør, TomTidslinje())
        val andelTilkjentYtelseTidslinje = andelTilkjentYtelseTidslinjer.getOrDefault(aktør, TomTidslinje())

        val utenlandskePeriodebeløpINorskeKroner = utenlandskePeriodebeløpTidslinje
            .tidspunktKombinerMed(valutakursTidslinje) { tidspunkt, upb, vk ->
                upb.tilMånedligValutabeløp(tidspunkt) * vk.tilKronerPerValutaenhet()
            }

        andelTilkjentYtelseTidslinje.kombinerMed(utenlandskePeriodebeløpINorskeKroner) { aty, beløp ->
            beløp?.let { aty.kalkulerFraUtenlandskPeriodebeløp(it) } ?: aty
        }
    }

    val barnasAndeler = barnasDifferanseberegnetAndelTilkjentYtelseTidslinjer.tilAndelerTilkjentYtelse()
    val søkersAndeler = tilkjentYtelse.søkersAndeler()

    validarSøkersYtelserMotEventueltNegativeAndelerForBarna(søkersAndeler, barnasAndeler)

    return tilkjentYtelse.medAndeler(søkersAndeler + barnasAndeler)
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
