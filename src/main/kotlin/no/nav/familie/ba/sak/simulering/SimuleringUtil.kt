package no.nav.familie.ba.sak.simulering

import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.simulering.domene.RestVedtakSimulering
import no.nav.familie.ba.sak.simulering.domene.SimuleringsPeriode
import no.nav.familie.ba.sak.simulering.domene.VedtakSimuleringMottaker
import no.nav.familie.ba.sak.simulering.domene.VedtakSimuleringPostering
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import java.math.BigDecimal
import java.time.LocalDate

fun filterBortUrelevanteVedtakSimuleringPosteringer(
        vedtakSimuleringMottakere: List<VedtakSimuleringMottaker>
): List<VedtakSimuleringMottaker> = vedtakSimuleringMottakere.map {
    it.copy(vedtakSimuleringPostering = it.vedtakSimuleringPostering.filter { postering ->
        postering.posteringType == PosteringType.FEILUTBETALING ||
        postering.posteringType == PosteringType.YTELSE
    })
}

fun vedtakSimuleringMottakereTilRestSimulering(vedtakSimuleringMottakere: List<VedtakSimuleringMottaker>): RestVedtakSimulering {
    val perioder = vedtakSimuleringMottakereTilSimuleringPerioder(vedtakSimuleringMottakere)
    val tidSimuleringHentet = vedtakSimuleringMottakere.first().opprettetTidspunkt.toLocalDate()

    val framtidigePerioder =
            perioder.filter {
                it.fom > tidSimuleringHentet ||
                (it.tom > tidSimuleringHentet && it.forfallsdato > tidSimuleringHentet)
            }

    val nestePeriode = framtidigePerioder.minByOrNull { it.fom }

    return RestVedtakSimulering(
            perioder = vedtakSimuleringMottakereTilSimuleringPerioder(vedtakSimuleringMottakere),
            fomDatoNestePeriode = nestePeriode?.fom,
            etterbetaling = hentTotalEtterbetaling(perioder, nestePeriode?.fom),
            feilutbetaling = hentTotalFeilutbetaling(perioder, nestePeriode?.fom),
            fom = perioder.minOf { it.fom },
            tomDatoNestePeriode = nestePeriode?.tom,
            forfallsdatoNestePeriode = nestePeriode?.forfallsdato,
            tidSimuleringHentet = tidSimuleringHentet
    )
}

fun vedtakSimuleringMottakereTilSimuleringPerioder(
        vedtakSimuleringMottakere: List<VedtakSimuleringMottaker>
): List<SimuleringsPeriode> {
    val simuleringPerioder = mutableMapOf<LocalDate, MutableList<VedtakSimuleringPostering>>()

    filterBortUrelevanteVedtakSimuleringPosteringer(vedtakSimuleringMottakere).forEach {
        it.vedtakSimuleringPostering.forEach { postering ->
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

fun hentNyttBeløpIPeriode(periode: List<VedtakSimuleringPostering>): BigDecimal {
    val sumPositiveYtelser = periode.filter { postering ->
        postering.posteringType == PosteringType.YTELSE && postering.beløp > BigDecimal.ZERO
    }.sumOf { it.beløp }
    val feilutbetaling = hentFeilbetalingIPeriode(periode)
    return if (feilutbetaling > BigDecimal.ZERO) sumPositiveYtelser - feilutbetaling else sumPositiveYtelser
}

fun hentFeilbetalingIPeriode(periode: List<VedtakSimuleringPostering>) =
        periode.filter { postering ->
            postering.posteringType == PosteringType.FEILUTBETALING
        }.sumOf { it.beløp }

fun hentTidligereUtbetaltIPeriode(periode: List<VedtakSimuleringPostering>): BigDecimal {
    val sumNegativeYtelser = periode.filter { postering ->
        (postering.posteringType === PosteringType.YTELSE && postering.beløp < BigDecimal.ZERO)
    }.sumOf { -it.beløp }
    val feilutbetaling = hentFeilbetalingIPeriode(periode)
    return if (feilutbetaling < BigDecimal.ZERO) sumNegativeYtelser - feilutbetaling else sumNegativeYtelser
}

fun hentResultatIPeriode(periode: List<VedtakSimuleringPostering>) =
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


fun SimuleringMottaker.tilVedtakSimuleringMottaker(vedtak: Vedtak): VedtakSimuleringMottaker {
    val vedtakSimuleringMottaker = VedtakSimuleringMottaker(
            mottakerNummer = this.mottakerNummer,
            mottakerType = this.mottakerType,
            vedtak = vedtak,
    )

    vedtakSimuleringMottaker.vedtakSimuleringPostering = this.simulertPostering.map {
        it.tilVedtakSimuleringPostering(vedtakSimuleringMottaker)
    }

    return vedtakSimuleringMottaker
}

fun SimulertPostering.tilVedtakSimuleringPostering(vedtakSimuleringMottaker: VedtakSimuleringMottaker) =
        VedtakSimuleringPostering(
                beløp = this.beløp,
                betalingType = this.betalingType,
                fagOmrådeKode = this.fagOmrådeKode,
                fom = this.fom,
                tom = this.tom,
                posteringType = this.posteringType,
                forfallsdato = this.forfallsdato,
                utenInntrekk = this.utenInntrekk,
                vedtakSimuleringMottaker = vedtakSimuleringMottaker,
        )