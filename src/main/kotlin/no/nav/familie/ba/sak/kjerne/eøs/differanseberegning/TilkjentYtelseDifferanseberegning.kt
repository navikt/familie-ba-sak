package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.erstattAndeler
import no.nav.familie.ba.sak.kjerne.beregning.domene.kopierMedUtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.tidslinje.eksperimentelt.filtrerIkkeNull
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

    val barnasDifferanseberegnetAndelTilkjentYtelseTidslinjer = alleBarna.associateWith { aktør ->
        val utenlandskePeriodebeløpTidslinje = utenlandskePeriodebeløpTidslinjer.getOrDefault(aktør, TomTidslinje())
        val valutakursTidslinje = valutakursTidslinjer.getOrDefault(aktør, TomTidslinje())
        val andelTilkjentYtelseTidslinje = andelTilkjentYtelseTidslinjer.getOrDefault(aktør, TomTidslinje())

        val utenlandskePeriodebeløpINorskeKroner = utenlandskePeriodebeløpTidslinje
            .tidspunktKombinerMed(valutakursTidslinje) { tidspunkt, upb, vk -> upb.multipliserMed(vk, tidspunkt) }

        andelTilkjentYtelseTidslinje
            .kombinerMed(utenlandskePeriodebeløpINorskeKroner) { aty, beløp ->
                beløp?.let { aty.kopierMedUtenlandskPeriodebeløp(it) } ?: aty
            }
            .filtrerIkkeNull()
    }

    val barnasDifferanseberegnedeAndeler = barnasDifferanseberegnetAndelTilkjentYtelseTidslinjer
        .values.flatMap { it.tilAndelTilkjentYtelse() }

    val søkersAndeler = tilkjentYtelse.andelerTilkjentYtelse
        .filter { it.erSøkersAndel() }

    validarSøkersYtelserMotEventueltNegativeAndeler(
        barnasDifferanseberegnedeAndeler,
        søkersAndeler
    )

    val barnasPositiveAndeler = barnasDifferanseberegnedeAndeler
        .filter { it.kalkulertUtbetalingsbeløp > 0 }

    return tilkjentYtelse.erstattAndeler(søkersAndeler + barnasPositiveAndeler)
}

private fun validarSøkersYtelserMotEventueltNegativeAndeler(
    barnasAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
    søkersAndelerTilkjentYtelse: List<AndelTilkjentYtelse>
) {
    val barnasSumNegativeUtebetalingsbeløp = barnasAndelerTilkjentYtelse
        .map { minOf(it.kalkulertUtbetalingsbeløp, 0) }
        .sum()

    val søkersSumUtebetalingsbeløp = søkersAndelerTilkjentYtelse
        .map { it.kalkulertUtbetalingsbeløp }
        .sum()

    if (barnasSumNegativeUtebetalingsbeløp < 0 && søkersSumUtebetalingsbeløp > 0)
        TODO(
            "Søker har småbarnstillegg og/elleer utvidet barnetrygd, " +
                "samtidig som ett eller flere barn har endt med negative utbetalingsbeløp etter differanseberegning. " +
                "Det er ikke støttet ennå"
        )
}
