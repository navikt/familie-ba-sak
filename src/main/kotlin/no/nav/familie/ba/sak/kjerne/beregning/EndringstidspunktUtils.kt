package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators

enum class BehandlingAlder {
    NY,
    GAMMEL,
}

data class AndelTilkjentYtelseEndringData(
    val aktør: Aktør,
    val kalkulertBeløp: Int,
    val behandlingAlder: BehandlingAlder,
)

fun List<AndelTilkjentYtelse>.hentPerioderMedEndringerFra(
    forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
): LocalDateTimeline<List<AndelTilkjentYtelseEndringData>> {
    val andelerTidslinje = this.hentTidslinje(BehandlingAlder.NY)
    val forrigeAndelerTidslinje = forrigeAndelerTilkjentYtelse.hentTidslinje(BehandlingAlder.GAMMEL)

    val kombinertTidslinje = andelerTidslinje.combine(
        forrigeAndelerTidslinje,
        StandardCombinators::bothValues,
        LocalDateTimeline.JoinStyle.CROSS_JOIN
    ) as LocalDateTimeline<List<AndelTilkjentYtelseEndringData>>

    return LocalDateTimeline(kombinertTidslinje.toSegments().filter { it.harEnEndring() })
}

private fun LocalDateSegment<List<AndelTilkjentYtelseEndringData>>.harEnEndring() =
    this.value
        .groupBy { endringsdata -> endringsdata.aktør.aktørId }
        .any { (_, nyOgGammelDataPåBrukerISegmentet) ->
            if (nyOgGammelDataPåBrukerISegmentet.size == 1) {
                true
            } else {
                val nySum = nyOgGammelDataPåBrukerISegmentet[0].kalkulertBeløp
                val forrigeSum = nyOgGammelDataPåBrukerISegmentet[1].kalkulertBeløp
                (nySum - forrigeSum) != 0
            }
        }

private fun List<AndelTilkjentYtelse>.hentTidslinje(behandlingAlder: BehandlingAlder):
    LocalDateTimeline<AndelTilkjentYtelseEndringData> = LocalDateTimeline(
    this.map {
        LocalDateSegment(
            it.stønadFom.førsteDagIInneværendeMåned(),
            it.stønadTom.sisteDagIInneværendeMåned(),
            AndelTilkjentYtelseEndringData(
                aktør = it.aktør,
                kalkulertBeløp = it.kalkulertUtbetalingsbeløp,
                behandlingAlder = behandlingAlder,
            )
        )
    }
)
