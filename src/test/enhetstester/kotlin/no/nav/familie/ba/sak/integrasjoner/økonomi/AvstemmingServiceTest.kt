package no.nav.familie.ba.sak.integrasjoner.økonomi

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.kontrakter.felles.oppdrag.PerioderForBehandling
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tidslinje.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class AvstemmingServiceTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val økonomiKlient = mockk<ØkonomiKlient>()
    private val beregningService = mockk<BeregningService>()
    private val taskService = mockk<TaskService>()
    private val batchRepository = mockk<BatchRepository>()
    private val dataChunkRepository = mockk<DataChunkRepository>()
    private val utbetalingsTidslinjeService = mockk<UtbetalingsTidslinjeService>()
    private val avstemmingService =
        AvstemmingService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            økonomiKlient = økonomiKlient,
            beregningService = beregningService,
            taskService = taskService,
            batchRepository = batchRepository,
            dataChunkRepository = dataChunkRepository,
            utbetalingsTidslinjeService = utbetalingsTidslinjeService,
        )

    @Nested
    inner class HentDataForKonsistensavstemmingVedHjelpAvUtbetalingstidslinjer {
        @Test
        fun `skal hente data for konsistensavstemming ved hjelp av utbetalingstidslinjer`() {
            // Arrange
            val avstemmingstidspunkt = LocalDateTime.of(2025, 2, 15, 0, 0, 0)
            val relevanteBehandlinger = listOf<Long>(1, 2)

            val person1 = lagPerson()
            val person2 = lagPerson()

            val tssIdentPerson1 = "12345"
            val tssIdentPerson2 = "12346"

            val utbetalingsperioder =
                listOf(
                    Periode(
                        verdi =
                            Utbetalingsperiode(
                                erEndringPåEksisterendePeriode = false,
                                opphør = null,
                                periodeId = 1,
                                forrigePeriodeId = 0,
                                datoForVedtak = LocalDate.now(),
                                klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                                vedtakdatoFom = LocalDate.of(2024, 2, 1),
                                vedtakdatoTom = LocalDate.of(2026, 10, 31),
                                sats = BigDecimal.valueOf(800),
                                satsType = Utbetalingsperiode.SatsType.MND,
                                utbetalesTil = person1.aktør.aktivFødselsnummer(),
                                behandlingId = 1,
                                utbetalingsgrad = null,
                            ),
                        fom = avstemmingstidspunkt.toLocalDate().førsteDagIInneværendeMåned(),
                        tom = LocalDate.of(2026, 10, 31),
                    ),
                    Periode(
                        verdi =
                            Utbetalingsperiode(
                                erEndringPåEksisterendePeriode = false,
                                opphør = null,
                                periodeId = 2,
                                forrigePeriodeId = null,
                                datoForVedtak = LocalDate.now(),
                                klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                                vedtakdatoFom = LocalDate.of(2024, 2, 1),
                                vedtakdatoTom = LocalDate.of(2026, 10, 31),
                                sats = BigDecimal.valueOf(800),
                                satsType = Utbetalingsperiode.SatsType.MND,
                                utbetalesTil = person2.aktør.aktivFødselsnummer(),
                                behandlingId = 2,
                                utbetalingsgrad = null,
                            ),
                        fom = avstemmingstidspunkt.toLocalDate().førsteDagIInneværendeMåned(),
                        tom = LocalDate.of(2026, 10, 31),
                    ),
                )

            every {
                utbetalingsTidslinjeService.genererUtbetalingsperioderForBehandlingerEtterDato(
                    behandlinger = relevanteBehandlinger,
                    dato = avstemmingstidspunkt.toLocalDate(),
                )
            } returns utbetalingsperioder

            every { behandlingHentOgPersisterService.hentAktivtFødselsnummerForBehandlinger(behandlingIder = relevanteBehandlinger) } returns
                mapOf(
                    1L to person1.aktør.aktivFødselsnummer(),
                    2L to person2.aktør.aktivFødselsnummer(),
                )

            every { behandlingHentOgPersisterService.hentTssEksternIdForBehandlinger(behandlingIder = relevanteBehandlinger) } returns
                mapOf(
                    1L to tssIdentPerson1,
                    2L to tssIdentPerson2,
                )
            // Act
            val perioderTilAvstemming =
                avstemmingService.hentDataForKonsistensavstemmingVedHjelpAvUtbetalingstidslinjer(
                    avstemmingstidspunkt = avstemmingstidspunkt,
                    relevanteBehandlinger = relevanteBehandlinger,
                )

            val forventedePerioderForBehandling1 =
                PerioderForBehandling(
                    behandlingId = "1",
                    perioder = setOf(1),
                    aktivFødselsnummer = person1.aktør.aktivFødselsnummer(),
                    utebetalesTil = tssIdentPerson1,
                )

            val forventedePerioderForBehandling2 =
                PerioderForBehandling(
                    behandlingId = "2",
                    perioder = setOf(2),
                    aktivFødselsnummer = person2.aktør.aktivFødselsnummer(),
                    utebetalesTil = tssIdentPerson2,
                )

            // Assert
            assertThat(perioderTilAvstemming).hasSize(2)
            assertThat(perioderTilAvstemming).containsExactlyInAnyOrder(forventedePerioderForBehandling1, forventedePerioderForBehandling2)
        }
    }
}
