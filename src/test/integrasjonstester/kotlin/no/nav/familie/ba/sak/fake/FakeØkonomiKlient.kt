package no.nav.familie.ba.sak.fake

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import org.springframework.web.client.RestOperations
import java.time.LocalDate

class FakeØkonomiKlient(
    restOperations: RestOperations,
) : ØkonomiKlient(familieOppdragUri = "http://familie-oppdrag-fake-uri", restOperations = restOperations, retryBackoffDelay = 1L) {
    override fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag): String = "Utbetalingsoppdrag iverksatt"

    override fun hentStatus(oppdragId: OppdragId): OppdragStatus = OppdragStatus.KVITTERT_OK

    override fun hentSimulering(utbetalingsoppdrag: Utbetalingsoppdrag): DetaljertSimuleringResultat = simuleringsresultater[utbetalingsoppdrag.saksnummer] ?: DetaljertSimuleringResultat(simuleringsMottakere)

    companion object {
        val simuleringsresultater = mutableMapOf<String, DetaljertSimuleringResultat>()

        fun leggTilSimuleringResultat(
            fagsakId: String,
            simuleringResultat: DetaljertSimuleringResultat,
        ) {
            simuleringsresultater[fagsakId] = simuleringResultat
        }
    }
}

val simuleringsPosteringer =
    listOf(
        SimulertPostering(
            fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
            fom = LocalDate.now().førsteDagIInneværendeMåned(),
            tom = LocalDate.now().sisteDagIMåned(),
            betalingType = BetalingType.DEBIT,
            beløp = 50.0.toBigDecimal(),
            posteringType = PosteringType.YTELSE,
            forfallsdato = LocalDate.now().plusYears(1),
            utenInntrekk = false,
            erFeilkonto = null,
        ),
        SimulertPostering(
            fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
            fom = LocalDate.now().førsteDagIInneværendeMåned(),
            tom = LocalDate.now().sisteDagIMåned(),
            betalingType = BetalingType.DEBIT,
            beløp = 1004.0.toBigDecimal(),
            posteringType = PosteringType.YTELSE,
            forfallsdato = LocalDate.now().plusYears(1),
            utenInntrekk = false,
            erFeilkonto = null,
        ),
        SimulertPostering(
            fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
            fom = LocalDate.now().førsteDagIInneværendeMåned(),
            tom = LocalDate.now().sisteDagIMåned(),
            betalingType = BetalingType.DEBIT,
            beløp = 50.0.toBigDecimal(),
            posteringType = PosteringType.FEILUTBETALING,
            forfallsdato = LocalDate.now().minusYears(1),
            utenInntrekk = false,
            erFeilkonto = null,
        ),
        SimulertPostering(
            fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
            fom = LocalDate.now().førsteDagIInneværendeMåned(),
            tom = LocalDate.now().sisteDagIMåned(),
            betalingType = BetalingType.KREDIT,
            beløp = (-50.0).toBigDecimal(),
            posteringType = PosteringType.MOTP,
            forfallsdato = LocalDate.now().plusYears(1),
            utenInntrekk = false,
            erFeilkonto = null,
        ),
        SimulertPostering(
            fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
            fom = LocalDate.now().førsteDagIInneværendeMåned(),
            tom = LocalDate.now().sisteDagIMåned(),
            betalingType = BetalingType.KREDIT,
            beløp = (-1054.0).toBigDecimal(),
            posteringType = PosteringType.YTELSE,
            forfallsdato = LocalDate.now().plusYears(1),
            utenInntrekk = false,
            erFeilkonto = null,
        ),
        SimulertPostering(
            fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
            fom = LocalDate.now().plusMonths(1).førsteDagIInneværendeMåned(),
            tom = LocalDate.now().plusMonths(1).sisteDagIMåned(),
            betalingType = BetalingType.DEBIT,
            beløp = 50.0.toBigDecimal(),
            posteringType = PosteringType.YTELSE,
            forfallsdato = LocalDate.now().plusYears(1),
            utenInntrekk = false,
            erFeilkonto = null,
        ),
        SimulertPostering(
            fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
            fom = LocalDate.now().plusMonths(1).førsteDagIInneværendeMåned(),
            tom = LocalDate.now().plusMonths(1).sisteDagIMåned(),
            betalingType = BetalingType.DEBIT,
            beløp = 1004.0.toBigDecimal(),
            posteringType = PosteringType.YTELSE,
            forfallsdato = LocalDate.now().plusYears(1),
            utenInntrekk = false,
            erFeilkonto = null,
        ),
        SimulertPostering(
            fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
            fom = LocalDate.now().plusMonths(1).førsteDagIInneværendeMåned(),
            tom = LocalDate.now().plusMonths(1).sisteDagIMåned(),
            betalingType = BetalingType.DEBIT,
            beløp = 50.0.toBigDecimal(),
            posteringType = PosteringType.FEILUTBETALING,
            forfallsdato = LocalDate.now().minusYears(1),
            utenInntrekk = false,
            erFeilkonto = null,
        ),
        SimulertPostering(
            fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
            fom = LocalDate.now().plusMonths(1).førsteDagIInneværendeMåned(),
            tom = LocalDate.now().plusMonths(1).sisteDagIMåned(),
            betalingType = BetalingType.KREDIT,
            beløp = (-50.0).toBigDecimal(),
            posteringType = PosteringType.MOTP,
            forfallsdato = LocalDate.now().plusYears(1),
            utenInntrekk = false,
            erFeilkonto = null,
        ),
        SimulertPostering(
            fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
            fom = LocalDate.now().plusMonths(1).førsteDagIInneværendeMåned(),
            tom = LocalDate.now().plusMonths(1).sisteDagIMåned(),
            betalingType = BetalingType.KREDIT,
            beløp = (-1054.0).toBigDecimal(),
            posteringType = PosteringType.YTELSE,
            forfallsdato = LocalDate.now().plusYears(1),
            utenInntrekk = false,
            erFeilkonto = null,
        ),
        SimulertPostering(
            fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
            fom = LocalDate.now().plusYears(1).førsteDagIInneværendeMåned(),
            tom = LocalDate.now().plusYears(1).sisteDagIMåned(),
            betalingType = BetalingType.DEBIT,
            beløp = 1054.0.toBigDecimal(),
            posteringType = PosteringType.YTELSE,
            forfallsdato = LocalDate.now().plusYears(2),
            utenInntrekk = false,
            erFeilkonto = null,
        ),
    )

val simuleringsMottakere =
    listOf(
        SimuleringMottaker(
            simulertPostering = simuleringsPosteringer,
            mottakerType = MottakerType.BRUKER,
            mottakerNummer = "12345678910",
        ),
    )
