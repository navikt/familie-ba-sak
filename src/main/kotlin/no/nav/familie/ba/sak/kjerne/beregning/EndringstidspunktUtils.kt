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

data class AndelTilkjentYtelseDataForÅKalkulereEndring(
    val aktør: Aktør,
    val kalkulertBeløp: Int,
    val behandlingAlder: BehandlingAlder,
)

data class AndelTilkjentYtelseEndringData(
    val aktør: Aktør,
    val beløpsdifferanse: Int,
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
    ) as LocalDateTimeline<List<AndelTilkjentYtelseDataForÅKalkulereEndring>>

    return LocalDateTimeline(kombinertTidslinje.toSegments().mapNotNull { it.tilSegmentMedDiffIBeløpPerPerson() })
}

private fun LocalDateSegment<List<AndelTilkjentYtelseDataForÅKalkulereEndring>>.tilSegmentMedDiffIBeløpPerPerson(): LocalDateSegment<List<AndelTilkjentYtelseEndringData>>? {
    val andelTilkjentYtelseEndringData = this.value
        .groupBy { endringsdata -> endringsdata.aktør.aktørId }
        .mapNotNull { (_, nyOgGammelDataPåBrukerISegmentet) ->
            val beløpsdifferanse = hentBeløpsendringPåPersonISegment(nyOgGammelDataPåBrukerISegmentet)

            if (beløpsdifferanse != 0) {
                AndelTilkjentYtelseEndringData(
                    aktør = nyOgGammelDataPåBrukerISegmentet[0].aktør,
                    beløpsdifferanse
                )
            } else null
        }

    return if (andelTilkjentYtelseEndringData.isEmpty()) {
        null
    } else LocalDateSegment(
        this.localDateInterval,
        andelTilkjentYtelseEndringData
    )
}

private fun hentBeløpsendringPåPersonISegment(nyOgGammelDataPåBrukerISegmentet: List<AndelTilkjentYtelseDataForÅKalkulereEndring>): Int {
    val nySum = nyOgGammelDataPåBrukerISegmentet
        .singleOrNull { it.behandlingAlder == BehandlingAlder.NY }
        ?.kalkulertBeløp ?: 0
    val forrigeSum = nyOgGammelDataPåBrukerISegmentet
        .singleOrNull { it.behandlingAlder == BehandlingAlder.GAMMEL }
        ?.kalkulertBeløp ?: 0

    return nySum - forrigeSum
}

private fun List<AndelTilkjentYtelse>.hentTidslinje(behandlingAlder: BehandlingAlder):
    LocalDateTimeline<AndelTilkjentYtelseDataForÅKalkulereEndring> = LocalDateTimeline(
    this.map {
        LocalDateSegment(
            it.stønadFom.førsteDagIInneværendeMåned(),
            it.stønadTom.sisteDagIInneværendeMåned(),
            AndelTilkjentYtelseDataForÅKalkulereEndring(
                aktør = it.aktør,
                kalkulertBeløp = it.kalkulertUtbetalingsbeløp,
                behandlingAlder = behandlingAlder,
            )
        )
    }
)
