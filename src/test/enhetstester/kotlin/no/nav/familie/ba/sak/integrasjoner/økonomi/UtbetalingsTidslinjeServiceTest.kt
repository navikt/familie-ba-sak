package no.nav.familie.ba.sak.integrasjoner.økonomi

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Opphør
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsperiode
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class UtbetalingsTidslinjeServiceTest {
    private val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()

    private val utbetalingsTidslinjeService = UtbetalingsTidslinjeService(tilkjentYtelseRepository = tilkjentYtelseRepository)

    @Nested
    inner class GenererUtbetalingsTidslinjerForFagsak {
        @Test
        fun `skal generere utbetalingstidslinjer for førstegangsbehandling med 2 kjeder ordinær barnetrygd`() {
            // Arrange
            val fagsak = lagFagsak()
            val behandling = lagBehandling(fagsak)
            val utbetalingsoppdrag = lagUtbetalingsoppdragFørstegangsbehandling(behandling.id)
            val tilkjentYtelse = lagTilkjentYtelse(behandling = behandling, utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag))

            every { tilkjentYtelseRepository.findByFagsak(fagsak.id) } returns listOf(tilkjentYtelse)

            // Act
            val utbetalingstidslinjer = utbetalingsTidslinjeService.genererUtbetalingstidslinjerForFagsak(fagsakId = fagsak.id)
            val førsteTidslinje = utbetalingsTidslinjeService.finnUtbetalingsTidslinjeForPeriodeId(1, utbetalingstidslinjer)
            val andreTidslinje = utbetalingsTidslinjeService.finnUtbetalingsTidslinjeForPeriodeId(2, utbetalingstidslinjer)

            // Assert
            assertThat(utbetalingstidslinjer).hasSize(2)
            assertThat(førsteTidslinje.tidslinje).isNotNull
            val perioderIFørsteTidslinje = førsteTidslinje.tidslinje.tilPerioderIkkeNull()
            assertThat(perioderIFørsteTidslinje).hasSize(2)
            assertThat(perioderIFørsteTidslinje[0].fom).isEqualTo(YearMonth.of(2024, 10).førsteDagIInneværendeMåned())
            assertThat(perioderIFørsteTidslinje[0].tom).isEqualTo(YearMonth.of(2025, 1).sisteDagIInneværendeMåned())
            assertThat(perioderIFørsteTidslinje[0].verdi.behandlingId).isEqualTo(behandling.id)
            assertThat(perioderIFørsteTidslinje[0].verdi.periodeId).isEqualTo(0)
            assertThat(perioderIFørsteTidslinje[0].verdi.forrigePeriodeId).isEqualTo(null)
            assertThat(perioderIFørsteTidslinje[1].fom).isEqualTo(YearMonth.of(2025, 2).førsteDagIInneværendeMåned())
            assertThat(perioderIFørsteTidslinje[1].tom).isEqualTo(YearMonth.of(2040, 9).sisteDagIInneværendeMåned())
            assertThat(perioderIFørsteTidslinje[1].verdi.behandlingId).isEqualTo(behandling.id)
            assertThat(perioderIFørsteTidslinje[1].verdi.periodeId).isEqualTo(1)
            assertThat(perioderIFørsteTidslinje[1].verdi.forrigePeriodeId).isEqualTo(0)

            assertThat(andreTidslinje.tidslinje).isNotNull
            val perioderIAndreTidslinje = andreTidslinje.tidslinje.tilPerioderIkkeNull()
            assertThat(perioderIAndreTidslinje).hasSize(1)
            assertThat(perioderIAndreTidslinje[0].fom).isEqualTo(YearMonth.of(2023, 4).førsteDagIInneværendeMåned())
            assertThat(perioderIAndreTidslinje[0].tom).isEqualTo(YearMonth.of(2040, 3).sisteDagIInneværendeMåned())
            assertThat(perioderIAndreTidslinje[0].verdi.behandlingId).isEqualTo(behandling.id)
            assertThat(perioderIAndreTidslinje[0].verdi.periodeId).isEqualTo(2)
            assertThat(perioderIAndreTidslinje[0].verdi.forrigePeriodeId).isEqualTo(null)
        }

        @Test
        fun `skal generere utbetalingstidslinjer for revurdering med endring på 1 kjede med ordinær barnetrygd`() {
            // Arrange
            val fagsak = lagFagsak()
            val førstegangsbehandling = lagBehandling(fagsak)
            val revurdering = lagBehandling(fagsak)

            every { tilkjentYtelseRepository.findByFagsak(fagsak.id) } returns
                listOf(
                    lagTilkjentYtelse(behandling = førstegangsbehandling, utbetalingsoppdrag = objectMapper.writeValueAsString(lagUtbetalingsoppdragFørstegangsbehandling(førstegangsbehandling.id))),
                    lagTilkjentYtelse(behandling = revurdering, utbetalingsoppdrag = objectMapper.writeValueAsString(lagUtbetalingsoppdragFørsteRevurdering(revurdering.id))),
                )

            // Act
            val utbetalingstidslinjer = utbetalingsTidslinjeService.genererUtbetalingstidslinjerForFagsak(fagsakId = fagsak.id)
            val førsteTidslinje = utbetalingsTidslinjeService.finnUtbetalingsTidslinjeForPeriodeId(3, utbetalingstidslinjer)
            val andreTidslinje = utbetalingsTidslinjeService.finnUtbetalingsTidslinjeForPeriodeId(2, utbetalingstidslinjer)

            // Assert
            assertThat(utbetalingstidslinjer).hasSize(2)
            assertThat(førsteTidslinje.tidslinje).isNotNull
            val perioderIFørsteTidslinje = førsteTidslinje.tidslinje.tilPerioderIkkeNull()
            assertThat(perioderIFørsteTidslinje).hasSize(2)
            assertThat(perioderIFørsteTidslinje[0].fom).isEqualTo(YearMonth.of(2024, 10).førsteDagIInneværendeMåned())
            assertThat(perioderIFørsteTidslinje[0].tom).isEqualTo(YearMonth.of(2024, 11).sisteDagIInneværendeMåned())
            assertThat(perioderIFørsteTidslinje[0].verdi.behandlingId).isEqualTo(førstegangsbehandling.id)
            assertThat(perioderIFørsteTidslinje[0].verdi.periodeId).isEqualTo(0)
            assertThat(perioderIFørsteTidslinje[0].verdi.forrigePeriodeId).isEqualTo(null)
            assertThat(perioderIFørsteTidslinje[1].fom).isEqualTo(YearMonth.of(2024, 12).førsteDagIInneværendeMåned())
            assertThat(perioderIFørsteTidslinje[1].tom).isEqualTo(YearMonth.of(2040, 9).sisteDagIInneværendeMåned())
            assertThat(perioderIFørsteTidslinje[1].verdi.behandlingId).isEqualTo(revurdering.id)
            assertThat(perioderIFørsteTidslinje[1].verdi.periodeId).isEqualTo(3)
            assertThat(perioderIFørsteTidslinje[1].verdi.forrigePeriodeId).isEqualTo(1)

            assertThat(andreTidslinje.tidslinje).isNotNull
            val perioderIAndreTidslinje = andreTidslinje.tidslinje.tilPerioderIkkeNull()
            assertThat(perioderIAndreTidslinje).hasSize(1)
            assertThat(perioderIAndreTidslinje[0].fom).isEqualTo(YearMonth.of(2023, 4).førsteDagIInneværendeMåned())
            assertThat(perioderIAndreTidslinje[0].tom).isEqualTo(YearMonth.of(2040, 3).sisteDagIInneværendeMåned())
            assertThat(perioderIAndreTidslinje[0].verdi.behandlingId).isEqualTo(førstegangsbehandling.id)
            assertThat(perioderIAndreTidslinje[0].verdi.periodeId).isEqualTo(2)
            assertThat(perioderIAndreTidslinje[0].verdi.forrigePeriodeId).isEqualTo(null)
        }

        @Test
        fun `skal generere utbetalingstidslinjer for revurdering med opphør på 1 kjede med ordinær barnetrygd`() {
            // Arrange
            val fagsak = lagFagsak()
            val førstegangsbehandling = lagBehandling(fagsak)
            val revurderingEndring = lagBehandling(fagsak)
            val revurderingOpphør = lagBehandling(fagsak)

            every { tilkjentYtelseRepository.findByFagsak(fagsak.id) } returns
                listOf(
                    lagTilkjentYtelse(behandling = førstegangsbehandling, utbetalingsoppdrag = objectMapper.writeValueAsString(lagUtbetalingsoppdragFørstegangsbehandling(førstegangsbehandling.id))),
                    lagTilkjentYtelse(behandling = revurderingEndring, utbetalingsoppdrag = objectMapper.writeValueAsString(lagUtbetalingsoppdragFørsteRevurdering(revurderingEndring.id))),
                    lagTilkjentYtelse(behandling = revurderingOpphør, utbetalingsoppdrag = objectMapper.writeValueAsString(lagUtbetalingsoppdragAndreRevurdering(revurderingOpphør.id))),
                )

            // Act
            val utbetalingstidslinjer = utbetalingsTidslinjeService.genererUtbetalingstidslinjerForFagsak(fagsakId = fagsak.id)
            val førsteTidslinje = utbetalingsTidslinjeService.finnUtbetalingsTidslinjeForPeriodeId(3, utbetalingstidslinjer)
            val andreTidslinje = utbetalingsTidslinjeService.finnUtbetalingsTidslinjeForPeriodeId(2, utbetalingstidslinjer)

            // Assert
            assertThat(utbetalingstidslinjer).hasSize(2)
            assertThat(førsteTidslinje.tidslinje).isNotNull
            val perioderIFørsteTidslinje = førsteTidslinje.tidslinje.tilPerioderIkkeNull()
            assertThat(perioderIFørsteTidslinje).hasSize(2)
            assertThat(perioderIFørsteTidslinje[0].fom).isEqualTo(YearMonth.of(2024, 10).førsteDagIInneværendeMåned())
            assertThat(perioderIFørsteTidslinje[0].tom).isEqualTo(YearMonth.of(2024, 11).sisteDagIInneværendeMåned())
            assertThat(perioderIFørsteTidslinje[0].verdi.behandlingId).isEqualTo(førstegangsbehandling.id)
            assertThat(perioderIFørsteTidslinje[0].verdi.periodeId).isEqualTo(0)
            assertThat(perioderIFørsteTidslinje[0].verdi.forrigePeriodeId).isEqualTo(null)
            assertThat(perioderIFørsteTidslinje[1].fom).isEqualTo(YearMonth.of(2024, 12).førsteDagIInneværendeMåned())
            assertThat(perioderIFørsteTidslinje[1].tom).isEqualTo(YearMonth.of(2040, 9).sisteDagIInneværendeMåned())
            assertThat(perioderIFørsteTidslinje[1].verdi.behandlingId).isEqualTo(revurderingEndring.id)
            assertThat(perioderIFørsteTidslinje[1].verdi.periodeId).isEqualTo(3)
            assertThat(perioderIFørsteTidslinje[1].verdi.forrigePeriodeId).isEqualTo(1)

            assertThat(andreTidslinje.tidslinje).isNotNull
            val perioderIAndreTidslinje = andreTidslinje.tidslinje.tilPerioderIkkeNull()
            assertThat(perioderIAndreTidslinje).hasSize(1)
            assertThat(perioderIAndreTidslinje[0].fom).isEqualTo(YearMonth.of(2023, 4).førsteDagIInneværendeMåned())
            assertThat(perioderIAndreTidslinje[0].tom).isEqualTo(YearMonth.of(2025, 1).sisteDagIInneværendeMåned())
            assertThat(perioderIAndreTidslinje[0].verdi.behandlingId).isEqualTo(revurderingOpphør.id)
            assertThat(perioderIAndreTidslinje[0].verdi.periodeId).isEqualTo(2)
            assertThat(perioderIAndreTidslinje[0].verdi.forrigePeriodeId).isEqualTo(null)
        }
    }
}

private fun lagUtbetalingsoppdragFørstegangsbehandling(behandlingId: Long): Utbetalingsoppdrag =
    lagUtbetalingsoppdrag(
        avstemmingTidspunkt = LocalDateTime.of(2025, 1, 1, 0, 0, 0),
        utbetalingsperiode =
            listOf(
                lagUtbetalingsperiode(
                    fom = YearMonth.of(2024, 10).førsteDagIInneværendeMåned(),
                    tom = YearMonth.of(2025, 1).sisteDagIInneværendeMåned(),
                    periodeId = 0,
                    forrigePeriodeId = null,
                    behandlingId = behandlingId,
                    klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                    beløp = BigDecimal.valueOf(1000L),
                ),
                lagUtbetalingsperiode(
                    fom = YearMonth.of(2025, 2).førsteDagIInneværendeMåned(),
                    tom = YearMonth.of(2040, 9).sisteDagIInneværendeMåned(),
                    periodeId = 1,
                    forrigePeriodeId = 0,
                    behandlingId = behandlingId,
                    klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                    beløp = BigDecimal.valueOf(500L),
                ),
                lagUtbetalingsperiode(
                    fom = YearMonth.of(2023, 4).førsteDagIInneværendeMåned(),
                    tom = YearMonth.of(2040, 3).sisteDagIInneværendeMåned(),
                    periodeId = 2,
                    forrigePeriodeId = null,
                    behandlingId = behandlingId,
                    klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                    beløp = BigDecimal.valueOf(1000L),
                ),
            ),
    )

private fun lagUtbetalingsoppdragFørsteRevurdering(behandlingId: Long): Utbetalingsoppdrag =
    lagUtbetalingsoppdrag(
        avstemmingTidspunkt = LocalDateTime.of(2025, 2, 1, 0, 0, 0),
        utbetalingsperiode =
            listOf(
                lagUtbetalingsperiode(
                    fom = YearMonth.of(2024, 12).førsteDagIInneværendeMåned(),
                    tom = YearMonth.of(2040, 9).sisteDagIInneværendeMåned(),
                    periodeId = 3,
                    forrigePeriodeId = 1,
                    behandlingId = behandlingId,
                    klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                    beløp = BigDecimal.valueOf(500L),
                ),
            ),
    )

private fun lagUtbetalingsoppdragAndreRevurdering(behandlingId: Long): Utbetalingsoppdrag =
    lagUtbetalingsoppdrag(
        avstemmingTidspunkt = LocalDateTime.of(2025, 3, 1, 0, 0, 0),
        utbetalingsperiode =
            listOf(
                lagUtbetalingsperiode(
                    fom = YearMonth.of(2023, 4).førsteDagIInneværendeMåned(),
                    tom = YearMonth.of(2040, 3).sisteDagIInneværendeMåned(),
                    periodeId = 2,
                    forrigePeriodeId = null,
                    behandlingId = behandlingId,
                    klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                    beløp = BigDecimal.valueOf(1000L),
                    opphør = Opphør(YearMonth.of(2025, 2).førsteDagIInneværendeMåned()),
                ),
            ),
    )

private fun lagUtbetalingsperiode(
    fom: LocalDate,
    tom: LocalDate,
    periodeId: Long,
    forrigePeriodeId: Long?,
    behandlingId: Long,
    klassifisering: String,
    beløp: BigDecimal,
    opphør: Opphør? = null,
) = Utbetalingsperiode(
    vedtakdatoFom = fom,
    vedtakdatoTom = tom,
    erEndringPåEksisterendePeriode = false,
    periodeId = periodeId,
    forrigePeriodeId = forrigePeriodeId,
    behandlingId = behandlingId,
    datoForVedtak = LocalDate.now(),
    klassifisering = klassifisering,
    sats = beløp,
    satsType = Utbetalingsperiode.SatsType.MND,
    utbetalesTil = "",
    opphør = opphør,
)
