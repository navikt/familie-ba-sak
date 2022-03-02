package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import java.time.LocalDate

enum class BehandlingAlder {
    NY,
    GAMMEL,
}

data class AndelTilkjentYtelseDataForÅKalkulereEndring(
    val aktør: Aktør,
    val kalkulertBeløp: Int,
    val behandlingAlder: BehandlingAlder,
)

typealias Beløpsdifferanse = Int
typealias AktørId = String

fun List<AndelTilkjentYtelse>.hentFørsteEndringstidspunkt(
    forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
): LocalDate? {
    return this.hentPerioderMedEndringerFra(forrigeAndelerTilkjentYtelse)
        .mapNotNull { (_, tidslinjeMedDifferanserPåPerson) -> tidslinjeMedDifferanserPåPerson.minOfOrNull { it.fom } }
        .minOfOrNull { it }
}

fun List<AndelTilkjentYtelse>.hentPerioderMedEndringerFra(
    forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
): Map<AktørId, LocalDateTimeline<Beløpsdifferanse>> {
    val andelerTidslinje = this.hentTidslinjerForPersoner(BehandlingAlder.NY)
    val forrigeAndelerTidslinje =
        forrigeAndelerTilkjentYtelse.hentTidslinjerForPersoner(BehandlingAlder.GAMMEL)

    val personerFraForrigeEllerDenneBehandlinger =
        (this.map { it.aktør.aktørId } + forrigeAndelerTilkjentYtelse.map { it.aktør.aktørId }).toSet()

    return personerFraForrigeEllerDenneBehandlinger.associateWith { aktørId ->
        val tidslinjeForPerson = andelerTidslinje[aktørId] ?: LocalDateTimeline(emptyList())
        val forrigeTidslinjeForPerson = forrigeAndelerTidslinje[aktørId] ?: LocalDateTimeline(emptyList())

        val kombinertTidslinje = tidslinjeForPerson.combine(
            forrigeTidslinjeForPerson,
            StandardCombinators::bothValues,
            LocalDateTimeline.JoinStyle.CROSS_JOIN
        ) as LocalDateTimeline<List<AndelTilkjentYtelseDataForÅKalkulereEndring>>

        LocalDateTimeline(
            kombinertTidslinje.toSegments().mapNotNull { it.tilSegmentMedDiffIBeløp() }
        )
    }
}

private fun LocalDateSegment<List<AndelTilkjentYtelseDataForÅKalkulereEndring>>.tilSegmentMedDiffIBeløp(): LocalDateSegment<Int>? {

    val beløpsdifferanse = hentBeløpsendringPåPersonISegment(this.value)

    return if (beløpsdifferanse != 0) {
        LocalDateSegment(
            this.localDateInterval,
            beløpsdifferanse
        )
    } else null
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

private fun List<AndelTilkjentYtelse>.hentTidslinjerForPersoner(behandlingAlder: BehandlingAlder):
    Map<String, LocalDateTimeline<AndelTilkjentYtelseDataForÅKalkulereEndring>> {

    return this.groupBy { it.aktør.aktørId }
        .map { (aktørId, andeler) ->
            aktørId to LocalDateTimeline(andeler.hentTidslinje(behandlingAlder))
        }
        .toMap()
}

private fun List<AndelTilkjentYtelse>.hentTidslinje(
    behandlingAlder: BehandlingAlder
) = map {
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
