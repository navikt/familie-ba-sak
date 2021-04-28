package no.nav.familie.ba.sak.config

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.økonomi.ØkonomiKlient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppdrag.*
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
import org.springframework.http.ResponseEntity
import java.time.LocalDate


@TestConfiguration
class ØkonomiTestConfig {

    @Bean
    @Profile("mock-økonomi")
    @Primary
    fun mockØkonomiKlient(): ØkonomiKlient {
        val økonomiKlient: ØkonomiKlient = mockk()

        val iverksettRespons =
                Ressurs("Mocksvar fra Økonomi-klient", Ressurs.Status.SUKSESS, "", "", null)
        every { økonomiKlient.iverksettOppdrag(any()) } returns iverksettRespons

        val hentStatusRespons =
                Ressurs(OppdragStatus.KVITTERT_OK, Ressurs.Status.SUKSESS, "", "", null)
        every { økonomiKlient.hentStatus(any()) } returns hentStatusRespons

        every { økonomiKlient.hentEtterbetalingsbeløp(any()) } returns Ressurs.success(RestSimulerResultat(etterbetaling = 1054))

        every { økonomiKlient.hentSimulering(any()) } returns Ressurs.success(DetaljertSimuleringResultat(simuleringMottakerMock))

        return økonomiKlient
    }
}


val simulertPosteringMock = listOf(
        SimulertPostering(
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                fom = LocalDate.parse("2019-09-01"),
                tom = LocalDate.parse("2019-09-30"),
                betalingType = BetalingType.DEBIT,
                beløp = 50.0.toBigDecimal(),
                posteringType = PosteringType.YTELSE,
                forfallsdato = LocalDate.parse("2021-02-23"),
                utenInntrekk = false,
        ),
        SimulertPostering(
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                fom = LocalDate.parse("2019-09-01"),
                tom = LocalDate.parse("2019-09-30"),
                betalingType = BetalingType.DEBIT,
                beløp = 1004.0.toBigDecimal(),
                posteringType = PosteringType.YTELSE,
                forfallsdato = LocalDate.parse("2021-02-23"),
                utenInntrekk = false,
        ),
        SimulertPostering(
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                fom = LocalDate.parse("2019-09-01"),
                tom = LocalDate.parse("2019-09-30"),
                betalingType = BetalingType.DEBIT,
                beløp = 50.0.toBigDecimal(),
                posteringType = PosteringType.FEILUTBETALING,
                forfallsdato = LocalDate.parse("2021-02-23"),
                utenInntrekk = false,
        ),
        SimulertPostering(
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                fom = LocalDate.parse("2019-09-01"),
                tom = LocalDate.parse("2019-09-30"),
                betalingType = BetalingType.KREDIT,
                beløp = -50.0.toBigDecimal(),
                posteringType = PosteringType.MOTP,
                forfallsdato = LocalDate.parse("2021-02-23"),
                utenInntrekk = false,
        ),
        SimulertPostering(
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                fom = LocalDate.parse("2019-09-01"),
                tom = LocalDate.parse("2019-09-30"),
                betalingType = BetalingType.KREDIT,
                beløp = -1054.0.toBigDecimal(),
                posteringType = PosteringType.YTELSE,
                forfallsdato = LocalDate.parse("2021-02-23"),
                utenInntrekk = false,
        ),
        SimulertPostering(
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                fom = LocalDate.parse("2019-10-01"),
                tom = LocalDate.parse("2019-10-31"),
                betalingType = BetalingType.DEBIT,
                beløp = 50.0.toBigDecimal(),
                posteringType = PosteringType.YTELSE,
                forfallsdato = LocalDate.parse("2021-02-23"),
                utenInntrekk = false,
        ),
        SimulertPostering(
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                fom = LocalDate.parse("2019-10-01"),
                tom = LocalDate.parse("2019-10-31"),
                betalingType = BetalingType.DEBIT,
                beløp = 1004.0.toBigDecimal(),
                posteringType = PosteringType.YTELSE,
                forfallsdato = LocalDate.parse("2021-02-23"),
                utenInntrekk = false,
        ),
        SimulertPostering(
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                fom = LocalDate.parse("2019-10-01"),
                tom = LocalDate.parse("2019-10-31"),
                betalingType = BetalingType.DEBIT,
                beløp = 50.0.toBigDecimal(),
                posteringType = PosteringType.FEILUTBETALING,
                forfallsdato = LocalDate.parse("2021-02-23"),
                utenInntrekk = false,
        ),
        SimulertPostering(
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                fom = LocalDate.parse("2019-10-01"),
                tom = LocalDate.parse("2019-10-31"),
                betalingType = BetalingType.KREDIT,
                beløp = -50.0.toBigDecimal(),
                posteringType = PosteringType.MOTP,
                forfallsdato = LocalDate.parse("2021-02-23"),
                utenInntrekk = false,
        ),
        SimulertPostering(
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                fom = LocalDate.parse("2019-10-01"),
                tom = LocalDate.parse("2019-10-31"),
                betalingType = BetalingType.KREDIT,
                beløp = -1054.0.toBigDecimal(),
                posteringType = PosteringType.YTELSE,
                forfallsdato = LocalDate.parse("2021-02-23"),
                utenInntrekk = false,
        ),
        SimulertPostering(
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                fom = LocalDate.parse("2021-04-01"),
                tom = LocalDate.parse("2021-04-30"),
                betalingType = BetalingType.DEBIT,
                beløp = 1054.0.toBigDecimal(),
                posteringType = PosteringType.YTELSE,
                forfallsdato = LocalDate.parse("2024-05-10"),
                utenInntrekk = false,
        ),
)

val simuleringMottakerMock = listOf(SimuleringMottaker(simulertPostering = simulertPosteringMock,
                                                       mottakerType = MottakerType.BRUKER,
                                                       mottakerNummer = "12345678910"))