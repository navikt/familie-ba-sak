package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.kjerne.behandlingsresultat.hentUtbetalingstidslinjeForSøker
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.felles.beregning.tilSeparateTidslinjerForBarna
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.fraOgMed
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.kombinerMed
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
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
    val endretUtbetalingÅrsaker: List<Årsak>,
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
            kombinertTidslinje.toSegments().mapNotNull { it.tilSegmentMedEndringer() }
        )
    }.filter { it.value.toSegments().isNotEmpty() }
}

fun Iterable<Kompetanse>.finnFørsteEndringstidspunkt(forrigeKompetansePerioder: Iterable<Kompetanse>): LocalDate {
    val separateTidslinjerForBarna = this.tilSeparateTidslinjerForBarna()
    val separateTidslinjerForBarnaForrigeBehandling = forrigeKompetansePerioder.tilSeparateTidslinjerForBarna()

    val unikeBarn = separateTidslinjerForBarna.keys + separateTidslinjerForBarnaForrigeBehandling.keys

    return unikeBarn.minOfOrNull { aktør ->
        val kompetansetidslinje: Tidslinje<Kompetanse, Måned>? =
            separateTidslinjerForBarna[aktør]
        val forrigeKompetansetidslinje: Tidslinje<Kompetanse, Måned>? =
            separateTidslinjerForBarnaForrigeBehandling[aktør]

        when {
            kompetansetidslinje == null -> forrigeKompetansetidslinje?.fraOgMed()?.tilLocalDate() ?: TIDENES_ENDE
            forrigeKompetansetidslinje == null -> kompetansetidslinje.fraOgMed().tilLocalDate()
            else -> kompetansetidslinje.finnFørsteEndringstidspunkt(forrigeKompetansetidslinje)
        }
    } ?: TIDENES_ENDE
}

fun Tidslinje<Kompetanse, Måned>.finnFørsteEndringstidspunkt(forrigeKompetansePerioder: Tidslinje<Kompetanse, Måned>): LocalDate {
    val kompetanseDiff = this
        .kombinerMed(forrigeKompetansePerioder) { nyKompetanse, forrigeKompetanse ->
            when {
                nyKompetanse == forrigeKompetanse -> null
                nyKompetanse == null -> forrigeKompetanse
                forrigeKompetanse == null -> nyKompetanse
                nyKompetanse != forrigeKompetanse -> nyKompetanse
                else -> null
            }
        }
    return kompetanseDiff.perioder().firstOrNull { it.innhold != null }?.fraOgMed?.tilLocalDate() ?: TIDENES_ENDE
}

private fun LocalDateSegment<List<AndelTilkjentYtelseDataForÅKalkulereEndring>>.tilSegmentMedEndringer(): LocalDateSegment<Int>? {
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

    val nyEndretUtbetalingÅrsaker =
        nyOgGammelDataPåBrukerISegmentet.find { it.behandlingAlder == BehandlingAlder.NY }?.endretUtbetalingÅrsaker?.sorted()
    val gammelEndretUtbetalingÅrsaker =
        nyOgGammelDataPåBrukerISegmentet.find { it.behandlingAlder == BehandlingAlder.GAMMEL }?.endretUtbetalingÅrsaker?.sorted()

    return nyttBeløp != gammeltBeløp || nyEndretUtbetalingÅrsaker != gammelEndretUtbetalingÅrsaker
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
                endretUtbetalingÅrsaker = it.endretUtbetalingAndeler.mapNotNull { endretUtbetalingAndel -> endretUtbetalingAndel.årsak },
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
                    endretUtbetalingÅrsaker = emptyList(), // TODO() her bør man nok prøve å hente overstyringer på søker også, men haster mest å fikse endringstidspunkt pga overstyringer på barn.
                    kalkulertBeløp = it.value
                )
            )
        }
    )
}
