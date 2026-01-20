package no.nav.familie.ba.sak.kjerne.simulering

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.simulering.domene.Simulering
import no.nav.familie.ba.sak.kjerne.simulering.domene.SimuleringsPeriode
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringPostering
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import java.math.BigDecimal
import java.time.LocalDate

fun filterBortUrelevanteVedtakSimuleringPosteringer(
    økonomiSimuleringMottakere: List<ØkonomiSimuleringMottaker>,
): List<ØkonomiSimuleringMottaker> =
    økonomiSimuleringMottakere.map {
        it.copy(
            økonomiSimuleringPostering =
                it.økonomiSimuleringPostering.filter { postering ->
                    postering.posteringType == PosteringType.FEILUTBETALING ||
                        postering.posteringType == PosteringType.YTELSE
                },
        )
    }

fun vedtakSimuleringMottakereTilSimuleringDto(
    økonomiSimuleringMottakere: List<ØkonomiSimuleringMottaker>,
): Simulering {
    val perioder =
        vedtakSimuleringMottakereTilSimuleringPerioder(
            økonomiSimuleringMottakere,
        )
    val tidSimuleringHentet = økonomiSimuleringMottakere.firstOrNull()?.opprettetTidspunkt?.toLocalDate()

    val framtidigePerioder =
        perioder.filter {
            it.fom > tidSimuleringHentet ||
                (it.tom > tidSimuleringHentet && it.forfallsdato > tidSimuleringHentet)
        }

    val nestePeriode = framtidigePerioder.filter { it.feilutbetaling == BigDecimal.ZERO }.minByOrNull { it.fom }
    val tomSisteUtbetaling =
        perioder.filter { nestePeriode == null || it.fom < nestePeriode.fom }.maxOfOrNull { it.tom }

    return Simulering(
        perioder = perioder,
        fomDatoNestePeriode = nestePeriode?.fom,
        etterbetaling = hentTotalEtterbetaling(perioder, nestePeriode?.fom),
        feilutbetaling =
            hentTotalFeilutbetaling(perioder, nestePeriode?.fom)
                .let { if (it < BigDecimal.ZERO) BigDecimal.ZERO else it },
        fom = perioder.minOfOrNull { it.fom },
        tomDatoNestePeriode = nestePeriode?.tom,
        forfallsdatoNestePeriode = nestePeriode?.forfallsdato,
        tidSimuleringHentet = tidSimuleringHentet,
        tomSisteUtbetaling = tomSisteUtbetaling,
    )
}

fun vedtakSimuleringMottakereTilSimuleringPerioder(
    økonomiSimuleringMottakere: List<ØkonomiSimuleringMottaker>,
): List<SimuleringsPeriode> {
    if (økonomiSimuleringMottakere.isEmpty()) {
        return emptyList()
    }
    val simuleringPerioder =
        filterBortUrelevanteVedtakSimuleringPosteringer(økonomiSimuleringMottakere)
            .flatMap { it.økonomiSimuleringPostering }
            .groupBy { it.fom }

    val tidSimuleringHentet = økonomiSimuleringMottakere.first().opprettetTidspunkt.toLocalDate()

    return simuleringPerioder.map { (fom, posteringListe) ->

        SimuleringsPeriode(
            fom = fom,
            tom = posteringListe[0].tom,
            forfallsdato = posteringListe[0].forfallsdato,
            nyttBeløp = hentNyttBeløpIPeriode(posteringListe),
            tidligereUtbetalt = hentTidligereUtbetaltIPeriode(posteringListe),
            resultat = hentResultatIPeriode(posteringListe),
            manuellPostering = hentManuellPosteringIPeriode(posteringListe),
            feilutbetaling = hentPositivFeilbetalingIPeriode(posteringListe),
            etterbetaling = hentEtterbetalingIPeriode(posteringListe, tidSimuleringHentet),
        )
    }
}

fun hentNyttBeløpIPeriode(periode: List<ØkonomiSimuleringPostering>): BigDecimal {
    val sumPositiveYtelser =
        periode
            .filter { it.posteringType == PosteringType.YTELSE }
            .filter { it.beløp > BigDecimal.ZERO }
            .filter { !it.erManuellPostering }
            .sumOf { it.beløp }
    val feilutbetaling = hentFeilutbetalingIPeriode(periode, false)

    return if (feilutbetaling > BigDecimal.ZERO) {
        sumPositiveYtelser - feilutbetaling
    } else {
        sumPositiveYtelser
    }
}

fun hentPositivFeilbetalingIPeriode(periode: List<ØkonomiSimuleringPostering>) =
    periode
        .filter { postering ->
            postering.posteringType == PosteringType.FEILUTBETALING &&
                postering.beløp > BigDecimal.ZERO
        }.sumOf { it.beløp }

fun hentNegativFeilutbetalingIPeriode(periode: List<ØkonomiSimuleringPostering>) =
    periode
        .filter { postering ->
            postering.posteringType == PosteringType.FEILUTBETALING &&
                postering.beløp < BigDecimal.ZERO
        }.sumOf { it.beløp }

fun hentFeilutbetalingIPeriode(
    periode: List<ØkonomiSimuleringPostering>,
    inkluderManuellePosteringer: Boolean,
) = periode
    .filter { it.posteringType == PosteringType.FEILUTBETALING }
    .filter { inkluderManuellePosteringer || !it.erManuellPostering }
    .sumOf { it.beløp }

fun hentTidligereUtbetaltIPeriode(periode: List<ØkonomiSimuleringPostering>): BigDecimal {
    val sumNegativeYtelser =
        periode
            .filter { it.posteringType == PosteringType.YTELSE }
            .filter { !it.erManuellPostering }
            .filter { it.beløp < BigDecimal.ZERO }
            .sumOf { it.beløp }

    val feilutbetaling = hentFeilutbetalingIPeriode(periode, false)

    // Manuelle posteringer brukes for å justere hva som faktisk skal bli betalt ut i en periode.
    // Endrer fortegn da en negativ sum skal øke tidligere utbetalt og en positiv sum redusere tidligere utbetalt.
    val sumManuellePosteringer = -hentManuellPosteringIPeriode(periode)

    return if (feilutbetaling < BigDecimal.ZERO) {
        -(sumNegativeYtelser - feilutbetaling)
    } else {
        -sumNegativeYtelser + sumManuellePosteringer
    }
}

fun hentManuellPosteringIPeriode(periode: List<ØkonomiSimuleringPostering>): BigDecimal {
    val sumManuellePosteringer =
        periode
            .filter { it.posteringType == PosteringType.YTELSE }
            .filter { it.erManuellPostering }
            .sumOf { it.beløp }

    val manuellFeilutbetaling = hentManuellFeilutbetalingIPeriode(periode)

    return sumManuellePosteringer - manuellFeilutbetaling
}

private fun hentManuellFeilutbetalingIPeriode(periode: List<ØkonomiSimuleringPostering>) =
    periode
        .filter { it.posteringType == PosteringType.FEILUTBETALING }
        .filter { it.erManuellPostering }
        .sumOf { it.beløp }

fun hentResultatIPeriode(periode: List<ØkonomiSimuleringPostering>): BigDecimal {
    val feilutbetaling = hentFeilutbetalingIPeriode(periode, true)

    return if (feilutbetaling > BigDecimal.ZERO) {
        -feilutbetaling
    } else {
        hentNyttBeløpIPeriode(periode) -
            hentTidligereUtbetaltIPeriode(periode)
    }
}

fun hentEtterbetalingIPeriode(
    periode: List<ØkonomiSimuleringPostering>,
    tidSimuleringHentet: LocalDate,
): BigDecimal {
    val periodeMedForfallFørTidSimuleringHentet = periode.filter { it.forfallsdato <= tidSimuleringHentet }
    val periodeHarPositivFeilutbetaling =
        hentFeilutbetalingIPeriode(periodeMedForfallFørTidSimuleringHentet, true) > BigDecimal.ZERO
    val resultat = hentResultatIPeriode(periodeMedForfallFørTidSimuleringHentet)

    return when {
        periodeHarPositivFeilutbetaling -> {
            BigDecimal.ZERO
        }

        else -> {
            maxOf(
                BigDecimal.ZERO,
                // Vi justerer etterbetalingsbeløp med negativ feilutbetaling i periode (redusert feilutbetaling).
                // Negative feilutbetalinger oppstår når man øker ytelsen i en periode det er registrert feilutbetaling på tidligere og tilbakekrevingsbehandlingen ikke er avsluttet.
                // Ved overførig til Oppdrag/økonomi vil registrert feilutbetaling bli redusert.
                // https://confluence.adeo.no/display/TFA/Tolkning+av+simulerte+posteringer+fra+oppdragsystemet
                (resultat + hentNegativFeilutbetalingIPeriode(periodeMedForfallFørTidSimuleringHentet)),
            )
        }
    }
}

fun hentTotalEtterbetaling(
    simuleringPerioder: List<SimuleringsPeriode>,
    fomDatoNestePeriode: LocalDate?,
): BigDecimal =
    simuleringPerioder
        .filter {
            (fomDatoNestePeriode == null || it.fom < fomDatoNestePeriode)
        }.sumOf { it.etterbetaling }
        .takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ZERO

fun hentTotalFeilutbetaling(
    simuleringPerioder: List<SimuleringsPeriode>,
    fomDatoNestePeriode: LocalDate?,
): BigDecimal =
    simuleringPerioder
        .filter { fomDatoNestePeriode == null || it.fom < fomDatoNestePeriode }
        .sumOf { it.feilutbetaling }

fun SimuleringMottaker.tilBehandlingSimuleringMottaker(behandling: Behandling): ØkonomiSimuleringMottaker {
    val behandlingSimuleringMottaker =
        ØkonomiSimuleringMottaker(
            mottakerNummer = this.mottakerNummer,
            mottakerType = this.mottakerType,
            behandling = behandling,
        )

    behandlingSimuleringMottaker.økonomiSimuleringPostering =
        this.simulertPostering.map {
            it.tilVedtakSimuleringPostering(behandlingSimuleringMottaker)
        }

    return behandlingSimuleringMottaker
}

fun SimulertPostering.tilVedtakSimuleringPostering(økonomiSimuleringMottaker: ØkonomiSimuleringMottaker) =
    ØkonomiSimuleringPostering(
        beløp = this.beløp,
        betalingType = this.betalingType,
        fagOmrådeKode = this.fagOmrådeKode,
        fom = this.fom,
        tom = this.tom,
        posteringType = this.posteringType,
        forfallsdato = this.forfallsdato,
        utenInntrekk = this.utenInntrekk,
        økonomiSimuleringMottaker = økonomiSimuleringMottaker,
        fagsakId = this.fagsakId?.trim()?.toLongOrNull(),
    )
