package no.nav.familie.ba.sak.simulering

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.simulering.domene.BrSimuleringMottaker
import no.nav.familie.ba.sak.simulering.domene.RestSimulering
import no.nav.familie.ba.sak.simulering.domene.SimuleringsPeriode
import no.nav.familie.ba.sak.simulering.domene.BrSimuleringPostering
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import java.math.BigDecimal
import java.time.LocalDate

fun filterBortUrelevanteVedtakSimuleringPosteringer(
        brSimuleringMottakere: List<BrSimuleringMottaker>
): List<BrSimuleringMottaker> = brSimuleringMottakere.map {
    it.copy(brSimuleringPostering = it.brSimuleringPostering.filter { postering ->
        postering.posteringType == PosteringType.FEILUTBETALING ||
        postering.posteringType == PosteringType.YTELSE
    })
}

fun vedtakSimuleringMottakereTilRestSimulering(brSimuleringMottakere: List<BrSimuleringMottaker>): RestSimulering {
    val perioder = vedtakSimuleringMottakereTilSimuleringPerioder(brSimuleringMottakere)
    val tidSimuleringHentet = brSimuleringMottakere.firstOrNull()?.opprettetTidspunkt?.toLocalDate()

    val framtidigePerioder =
            perioder.filter {
                it.fom > tidSimuleringHentet ||
                (it.tom > tidSimuleringHentet && it.forfallsdato > tidSimuleringHentet)
            }

    val nestePeriode = framtidigePerioder.minByOrNull { it.fom }

    return RestSimulering(
            perioder = vedtakSimuleringMottakereTilSimuleringPerioder(brSimuleringMottakere),
            fomDatoNestePeriode = nestePeriode?.fom,
            etterbetaling = hentTotalEtterbetaling(perioder, nestePeriode?.fom),
            feilutbetaling = hentTotalFeilutbetaling(perioder, nestePeriode?.fom),
            fom = perioder.minOfOrNull { it.fom },
            tomDatoNestePeriode = nestePeriode?.tom,
            forfallsdatoNestePeriode = nestePeriode?.forfallsdato,
            tidSimuleringHentet = tidSimuleringHentet
    )
}

fun vedtakSimuleringMottakereTilSimuleringPerioder(
        brSimuleringMottakere: List<BrSimuleringMottaker>
): List<SimuleringsPeriode> {
    val simuleringPerioder = mutableMapOf<LocalDate, MutableList<BrSimuleringPostering>>()

    filterBortUrelevanteVedtakSimuleringPosteringer(brSimuleringMottakere).forEach {
        it.brSimuleringPostering.forEach { postering ->
            if (simuleringPerioder.containsKey(postering.fom))
                simuleringPerioder[postering.fom]?.add(postering)
            else simuleringPerioder[postering.fom] = mutableListOf(postering)
        }
    }

    return simuleringPerioder.map { (fom, posteringListe) ->
        SimuleringsPeriode(
                fom,
                posteringListe[0].tom,
                posteringListe[0].forfallsdato,
                nyttBeløp = hentNyttBeløpIPeriode(posteringListe),
                tidligereUtbetalt = hentTidligereUtbetaltIPeriode(posteringListe),
                resultat = hentResultatIPeriode(posteringListe),
                feilutbetaling = hentFeilbetalingIPeriode(posteringListe),
        )
    }
}

fun hentNyttBeløpIPeriode(periode: List<BrSimuleringPostering>): BigDecimal {
    val sumPositiveYtelser = periode.filter { postering ->
        postering.posteringType == PosteringType.YTELSE && postering.beløp > BigDecimal.ZERO
    }.sumOf { it.beløp }
    val feilutbetaling = hentFeilbetalingIPeriode(periode)
    return if (feilutbetaling > BigDecimal.ZERO) sumPositiveYtelser - feilutbetaling else sumPositiveYtelser
}

fun hentFeilbetalingIPeriode(periode: List<BrSimuleringPostering>) =
        periode.filter { postering ->
            postering.posteringType == PosteringType.FEILUTBETALING
        }.sumOf { it.beløp }

fun hentTidligereUtbetaltIPeriode(periode: List<BrSimuleringPostering>): BigDecimal {
    val sumNegativeYtelser = periode.filter { postering ->
        (postering.posteringType === PosteringType.YTELSE && postering.beløp < BigDecimal.ZERO)
    }.sumOf { -it.beløp }
    val feilutbetaling = hentFeilbetalingIPeriode(periode)
    return if (feilutbetaling < BigDecimal.ZERO) sumNegativeYtelser - feilutbetaling else sumNegativeYtelser
}

fun hentResultatIPeriode(periode: List<BrSimuleringPostering>) =
        if (periode.map { it.posteringType }.contains(PosteringType.FEILUTBETALING)) {
            periode.filter {
                it.posteringType == PosteringType.FEILUTBETALING
            }.sumOf { -it.beløp }
        } else
            periode.sumOf { it.beløp }

fun hentTotalEtterbetaling(simuleringPerioder: List<SimuleringsPeriode>, fomDatoNestePeriode: LocalDate?) =
        simuleringPerioder.filter {
            it.resultat > BigDecimal.ZERO && (fomDatoNestePeriode == null || it.fom < fomDatoNestePeriode)
        }.sumOf { it.resultat }


fun hentTotalFeilutbetaling(simuleringPerioder: List<SimuleringsPeriode>, fomDatoNestePeriode: LocalDate?) =
        simuleringPerioder.filter { fomDatoNestePeriode == null || it.fom < fomDatoNestePeriode }.sumOf { it.feilutbetaling }


fun SimuleringMottaker.tilBehandlingSimuleringMottaker(behandling: Behandling): BrSimuleringMottaker {
    val behandlingSimuleringMottaker = BrSimuleringMottaker(
            mottakerNummer = this.mottakerNummer,
            mottakerType = this.mottakerType,
            behandling = behandling,
    )

    behandlingSimuleringMottaker.brSimuleringPostering = this.simulertPostering.map {
        it.tilVedtakSimuleringPostering(behandlingSimuleringMottaker)
    }

    return behandlingSimuleringMottaker
}

fun SimulertPostering.tilVedtakSimuleringPostering(brSimuleringMottaker: BrSimuleringMottaker) =
        BrSimuleringPostering(
                beløp = this.beløp,
                betalingType = this.betalingType,
                fagOmrådeKode = this.fagOmrådeKode,
                fom = this.fom,
                tom = this.tom,
                posteringType = this.posteringType,
                forfallsdato = this.forfallsdato,
                utenInntrekk = this.utenInntrekk,
                brSimuleringMottaker = brSimuleringMottaker,
        )