package no.nav.familie.ba.sak.kjerne.simulering

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.simulering.domene.RestSimulering
import no.nav.familie.ba.sak.kjerne.simulering.domene.SimuleringsPeriode
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringPostering
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import java.math.BigDecimal
import java.time.LocalDate

fun filterBortUrelevanteVedtakSimuleringPosteringer(
    økonomiSimuleringMottakere: List<ØkonomiSimuleringMottaker>
): List<ØkonomiSimuleringMottaker> = økonomiSimuleringMottakere.map {
    it.copy(
        økonomiSimuleringPostering = it.økonomiSimuleringPostering.filter { postering ->
            postering.posteringType == PosteringType.FEILUTBETALING ||
                postering.posteringType == PosteringType.YTELSE
        }
    )
}

fun vedtakSimuleringMottakereTilRestSimulering(
    økonomiSimuleringMottakere: List<ØkonomiSimuleringMottaker>,
    erManuelPosteringTogglePå: Boolean
): RestSimulering {
    val perioder = vedtakSimuleringMottakereTilSimuleringPerioder(økonomiSimuleringMottakere, erManuelPosteringTogglePå)
    val tidSimuleringHentet = økonomiSimuleringMottakere.firstOrNull()?.opprettetTidspunkt?.toLocalDate()

    val framtidigePerioder =
        perioder.filter {
            it.fom > tidSimuleringHentet ||
                (it.tom > tidSimuleringHentet && it.forfallsdato > tidSimuleringHentet)
        }

    val nestePeriode = framtidigePerioder.filter { it.feilutbetaling == BigDecimal.ZERO }.minByOrNull { it.fom }
    val tomSisteUtbetaling =
        perioder.filter { nestePeriode == null || it.fom < nestePeriode.fom }.maxOfOrNull { it.tom }

    return RestSimulering(
        perioder = perioder,
        fomDatoNestePeriode = nestePeriode?.fom,
        etterbetaling = hentTotalEtterbetaling(perioder, nestePeriode?.fom),
        feilutbetaling = hentTotalFeilutbetaling(perioder, nestePeriode?.fom)
            .let { if (it < BigDecimal.ZERO) BigDecimal.ZERO else it },
        fom = perioder.minOfOrNull { it.fom },
        tomDatoNestePeriode = nestePeriode?.tom,
        forfallsdatoNestePeriode = nestePeriode?.forfallsdato,
        tidSimuleringHentet = tidSimuleringHentet,
        tomSisteUtbetaling = tomSisteUtbetaling
    )
}

fun vedtakSimuleringMottakereTilSimuleringPerioder(
    økonomiSimuleringMottakere: List<ØkonomiSimuleringMottaker>,
    erManuelPosteringTogglePå: Boolean
): List<SimuleringsPeriode> {
    val simuleringPerioder = mutableMapOf<LocalDate, MutableList<ØkonomiSimuleringPostering>>()

    filterBortUrelevanteVedtakSimuleringPosteringer(økonomiSimuleringMottakere).forEach {
        it.økonomiSimuleringPostering.forEach { postering ->
            if (simuleringPerioder.containsKey(postering.fom)) {
                simuleringPerioder[postering.fom]?.add(postering)
            } else {
                simuleringPerioder[postering.fom] = mutableListOf(postering)
            }
        }
    }

    val tidSimuleringHentet = økonomiSimuleringMottakere.firstOrNull()?.opprettetTidspunkt?.toLocalDate()

    return simuleringPerioder.map { (fom, posteringListe) ->
        SimuleringsPeriode(
            fom,
            posteringListe[0].tom,
            posteringListe[0].forfallsdato,
            nyttBeløp = hentNyttBeløpIPeriode(posteringListe),
            tidligereUtbetalt = if (erManuelPosteringTogglePå) {
                hentTidligereUtbetaltIPeriode(posteringListe)
            } else {
                hentTidligereUtbetaltIPeriodeGammel(posteringListe)
            },
            resultat = if (erManuelPosteringTogglePå) {
                hentResultatIPeriode(posteringListe)
            } else {
                hentResultatIPeriodeGammel(posteringListe)
            },
            manuellPostering = hentManuellPosteringIPeriode(posteringListe),
            feilutbetaling = hentPositivFeilbetalingIPeriode(posteringListe),
            etterbetaling = if (erManuelPosteringTogglePå) {
                hentEtterbetalingIPeriode(posteringListe, tidSimuleringHentet)
            } else {
                hentEtterbetalingIPeriodeGammel(posteringListe, tidSimuleringHentet)
            }
        )
    }
}

fun hentNyttBeløpIPeriode(periode: List<ØkonomiSimuleringPostering>): BigDecimal {
    val sumPositiveYtelser = periode.filter { postering ->
        postering.posteringType == PosteringType.YTELSE &&
            postering.beløp > BigDecimal.ZERO &&
            postering.fagOmrådeKode != FagOmrådeKode.BARNETRYGD_INFOTRYGD_MANUELT
    }.sumOf { it.beløp }
    val feilutbetaling = hentFeilbetalingIPeriode(periode)
    return if (feilutbetaling > BigDecimal.ZERO) sumPositiveYtelser - feilutbetaling else sumPositiveYtelser
}

fun hentFeilbetalingIPeriode(periode: List<ØkonomiSimuleringPostering>) =
    periode.filter { postering ->
        postering.posteringType == PosteringType.FEILUTBETALING
    }.sumOf { it.beløp }

fun hentPositivFeilbetalingIPeriode(periode: List<ØkonomiSimuleringPostering>) =
    periode.filter { postering ->
        postering.posteringType == PosteringType.FEILUTBETALING &&
            postering.beløp > BigDecimal.ZERO
    }.sumOf { it.beløp }

@Deprecated("Skal bruke hentTidligereUtbetaltIPeriode når manuelle posteringer er tester ferdig")
fun hentTidligereUtbetaltIPeriodeGammel(periode: List<ØkonomiSimuleringPostering>): BigDecimal {
    val sumNegativeYtelser = periode.filter { postering ->
        (postering.posteringType == PosteringType.YTELSE && postering.beløp < BigDecimal.ZERO)
    }.sumOf { it.beløp }
    val feilutbetaling = hentFeilbetalingIPeriode(periode)
    return if (feilutbetaling < BigDecimal.ZERO) -(sumNegativeYtelser - feilutbetaling) else -sumNegativeYtelser
}

fun hentTidligereUtbetaltIPeriode(periode: List<ØkonomiSimuleringPostering>): BigDecimal {
    val sumNegativeYtelser = periode.filter { postering ->
        (postering.posteringType == PosteringType.YTELSE && postering.beløp < BigDecimal.ZERO)
    }.sumOf { it.beløp }
    val feilutbetaling = hentFeilbetalingIPeriode(periode)
    return if (feilutbetaling < BigDecimal.ZERO) {
        -(sumNegativeYtelser - feilutbetaling)
    } else {
        -sumNegativeYtelser - hentManuellPosteringIPeriode(
            periode
        )
    }
}

fun hentManuellPosteringIPeriode(periode: List<ØkonomiSimuleringPostering>): BigDecimal {
    return periode.filter { postering ->
        postering.posteringType == PosteringType.YTELSE &&
            postering.fagOmrådeKode == FagOmrådeKode.BARNETRYGD_INFOTRYGD_MANUELT
    }.sumOf { it.beløp }
}

@Deprecated("Skal bruke hentResultatIPeriode når manuelle posteringer er tester ferdig")
fun hentResultatIPeriodeGammel(periode: List<ØkonomiSimuleringPostering>): BigDecimal {
    val feilutbetaling = hentFeilbetalingIPeriode(periode)
    return if (feilutbetaling > BigDecimal.ZERO) {
        -feilutbetaling
    } else {
        hentNyttBeløpIPeriode(periode) - hentTidligereUtbetaltIPeriodeGammel(periode)
    }
}

fun hentResultatIPeriode(periode: List<ØkonomiSimuleringPostering>): BigDecimal {
    val feilutbetaling = hentFeilbetalingIPeriode(periode)

    return if (feilutbetaling > BigDecimal.ZERO) {
        -feilutbetaling
    } else {
        hentNyttBeløpIPeriode(periode) - hentTidligereUtbetaltIPeriode(periode) - hentManuellPosteringIPeriode(
            periode
        )
    }
}

@Deprecated("Skal bruke hentEtterbetalingIPeriode når manuelle posteringer er tester ferdig")
fun hentEtterbetalingIPeriodeGammel(
    periode: List<ØkonomiSimuleringPostering>,
    tidSimuleringHentet: LocalDate?
): BigDecimal {
    val periodeHarPositivFeilutbetaling =
        periode.any { it.posteringType == PosteringType.FEILUTBETALING && it.beløp > BigDecimal.ZERO }
    val sumYtelser =
        periode.filter { it.posteringType == PosteringType.YTELSE && it.forfallsdato <= tidSimuleringHentet }
            .sumOf { it.beløp }
    return when {
        periodeHarPositivFeilutbetaling ->
            BigDecimal.ZERO

        else ->
            if (sumYtelser < BigDecimal.ZERO) {
                BigDecimal.ZERO
            } else {
                sumYtelser
            }
    }
}

fun hentEtterbetalingIPeriode(
    periode: List<ØkonomiSimuleringPostering>,
    tidSimuleringHentet: LocalDate?
): BigDecimal {
    val periodeHarPositivFeilutbetaling =
        periode.any { it.posteringType == PosteringType.FEILUTBETALING && it.beløp > BigDecimal.ZERO }
    val sumYtelser =
        periode.filter { it.posteringType == PosteringType.YTELSE && it.forfallsdato <= tidSimuleringHentet }
            .sumOf { it.beløp }
    val sumManuellePosteringer = hentManuellPosteringIPeriode(periode)
    return when {
        periodeHarPositivFeilutbetaling ->
            BigDecimal.ZERO

        else ->

            sumYtelser - sumManuellePosteringer
    }
}

fun hentTotalEtterbetaling(simuleringPerioder: List<SimuleringsPeriode>, fomDatoNestePeriode: LocalDate?): BigDecimal {
    return simuleringPerioder.filter {
        (fomDatoNestePeriode == null || it.fom < fomDatoNestePeriode)
    }.sumOf { it.etterbetaling }.takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ZERO
}

fun hentTotalFeilutbetaling(simuleringPerioder: List<SimuleringsPeriode>, fomDatoNestePeriode: LocalDate?): BigDecimal {
    return simuleringPerioder
        .filter { fomDatoNestePeriode == null || it.fom < fomDatoNestePeriode }
        .sumOf { it.feilutbetaling }
}

fun SimuleringMottaker.tilBehandlingSimuleringMottaker(behandling: Behandling): ØkonomiSimuleringMottaker {
    val behandlingSimuleringMottaker = ØkonomiSimuleringMottaker(
        mottakerNummer = this.mottakerNummer,
        mottakerType = this.mottakerType,
        behandling = behandling
    )

    behandlingSimuleringMottaker.økonomiSimuleringPostering = this.simulertPostering.map {
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
        økonomiSimuleringMottaker = økonomiSimuleringMottaker
    )
