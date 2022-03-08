package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.hentUtbetalingstidslinjeForSøker
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.fpsak.tidsserie.LocalDateSegment
import no.nav.fpsak.tidsserie.LocalDateTimeline
import no.nav.fpsak.tidsserie.StandardCombinators
import java.time.LocalDate

enum class BehandlingAlder {
    NY,
    GAMMEL,
}

typealias Beløpsdifferanse = Int
typealias AktørId = String

data class AndelTilkjentYtelseDataForÅKalkulereEndring(
    val aktørId: AktørId,
    val kalkulertBeløp: Int,
    val behandlingAlder: BehandlingAlder,
)

fun List<AndelTilkjentYtelse>.hentFørsteEndringstidspunkt(
    forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse>,
): LocalDate? = this.hentPerioderMedEndringerFra(forrigeAndelerTilkjentYtelse)
    .mapNotNull { (_, tidslinjeMedDifferanserPåPerson) -> tidslinjeMedDifferanserPåPerson.minOfOrNull { it.fom } }
    .minOfOrNull { it }

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
    val erEndring = erEndringPåPersonISegment(this.value)

    return if (erEndring) {
        LocalDateSegment(
            this.localDateInterval,
            hentBeløpsendringPåPersonISegment(this.value)
        )
    } else null
}

private fun erEndringPåPersonISegment(nyOgGammelDataPåBrukerISegmentet: List<AndelTilkjentYtelseDataForÅKalkulereEndring>): Boolean {
    val nyttBeløp = nyOgGammelDataPåBrukerISegmentet.finnKalkulertBeløp(BehandlingAlder.NY)
    val gammeltBeløp = nyOgGammelDataPåBrukerISegmentet.finnKalkulertBeløp(BehandlingAlder.GAMMEL)

    return nyttBeløp != gammeltBeløp
}

private fun hentBeløpsendringPåPersonISegment(nyOgGammelDataPåBrukerISegmentet: List<AndelTilkjentYtelseDataForÅKalkulereEndring>): Int {
    val nyttBeløp = nyOgGammelDataPåBrukerISegmentet.finnKalkulertBeløp(BehandlingAlder.NY) ?: 0
    val gammeltBeløp = nyOgGammelDataPåBrukerISegmentet.finnKalkulertBeløp(BehandlingAlder.GAMMEL) ?: 0

    return nyttBeløp - gammeltBeløp
}

private fun List<AndelTilkjentYtelseDataForÅKalkulereEndring>.finnKalkulertBeløp(behandlingAlder: BehandlingAlder) =
    singleOrNull { it.behandlingAlder == behandlingAlder }
        ?.kalkulertBeløp

private fun List<AndelTilkjentYtelse>.hentTidslinjerForPersoner(behandlingAlder: BehandlingAlder):
    Map<String, LocalDateTimeline<AndelTilkjentYtelseDataForÅKalkulereEndring>> {

    return this.groupBy { it.aktør.aktørId }
        .map { (aktørId, andeler) ->
            if (andeler.any { it.erSøkersAndel() }) {
                aktørId to kombinerOverlappendeAndelerForSøker(
                    andeler = andeler,
                    behandlingAlder = behandlingAlder,
                    aktørId = aktørId
                )
            } else aktørId to andeler.hentTidslinje(behandlingAlder)
        }.toMap()
}

private fun List<AndelTilkjentYtelse>.hentTidslinje(
    behandlingAlder: BehandlingAlder
): LocalDateTimeline<AndelTilkjentYtelseDataForÅKalkulereEndring> = LocalDateTimeline(
    map {
        LocalDateSegment(
            it.stønadFom.førsteDagIInneværendeMåned(),
            it.stønadTom.sisteDagIInneværendeMåned(),
            AndelTilkjentYtelseDataForÅKalkulereEndring(
                aktørId = it.aktør.aktørId,
                kalkulertBeløp = it.kalkulertUtbetalingsbeløp,
                behandlingAlder = behandlingAlder,
            )
        )
    }
)

private fun kombinerOverlappendeAndelerForSøker(
    andeler: List<AndelTilkjentYtelse>,
    behandlingAlder: BehandlingAlder,
    aktørId: AktørId
): LocalDateTimeline<AndelTilkjentYtelseDataForÅKalkulereEndring> {
    val segmenter = hentUtbetalingstidslinjeForSøker(andeler).toSegments()

    return LocalDateTimeline(
        segmenter.map {
            LocalDateSegment(
                it.localDateInterval,
                AndelTilkjentYtelseDataForÅKalkulereEndring(
                    aktørId = aktørId,
                    behandlingAlder = behandlingAlder,
                    kalkulertBeløp = it.value
                )
            )
        }
    )
}
