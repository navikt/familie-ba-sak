package no.nav.familie.ba.sak.mock

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@TestConfiguration
class ØkonomiTestConfig {
    @Bean
    @Profile("mock-økonomi")
    @Primary
    fun mockØkonomiKlient(): ØkonomiKlient {
        val økonomiKlient: ØkonomiKlient = mockk()

        clearØkonomiMocks(økonomiKlient)

        return økonomiKlient
    }

    companion object {
        fun clearØkonomiMocks(økonomiKlient: ØkonomiKlient) {
            clearMocks(økonomiKlient)

            val iverksettRespons = "Mocksvar fra Økonomi-klient"
            every { økonomiKlient.iverksettOppdrag(any()) } returns iverksettRespons

            val hentStatusRespons = OppdragStatus.KVITTERT_OK

            every { økonomiKlient.hentStatus(any()) } returns hentStatusRespons

            every { økonomiKlient.hentSimulering(any()) } returns DetaljertSimuleringResultat(simuleringMottakerMock)
        }
    }
}

val simulertPosteringMock =
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

val simuleringMottakerMock =
    listOf(
        SimuleringMottaker(
            simulertPostering = simulertPosteringMock,
            mottakerType = MottakerType.BRUKER,
            mottakerNummer = "12345678910",
        ),
    )
