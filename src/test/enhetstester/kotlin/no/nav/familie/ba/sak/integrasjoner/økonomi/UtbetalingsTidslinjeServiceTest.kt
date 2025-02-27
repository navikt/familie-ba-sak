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

    private val førstePeriodeIdKjede1 = 0L
    private val førstePeriodeIdKjede2 = 2L
    private val fagsak = lagFagsak()
    private val førstegangsbehandling = lagBehandling(fagsak)

    @Nested
    inner class GenererUtbetalingsTidslinjerForFagsak {
        @Test
        fun `skal generere utbetalingstidslinjer for førstegangsbehandling med 2 kjeder ordinær barnetrygd`() {
            // Arrange
            val tilkjentYtelse = lagTilkjentYtelse(behandling = førstegangsbehandling, utbetalingsoppdrag = objectMapper.writeValueAsString(lagUtbetalingsoppdragFørstegangsbehandling()))

            every { tilkjentYtelseRepository.findByFagsak(fagsak.id) } returns listOf(tilkjentYtelse)

            // Act
            val utbetalingstidslinjer = utbetalingsTidslinjeService.genererUtbetalingstidslinjerForFagsak(fagsakId = fagsak.id)
            val førsteTidslinje = utbetalingsTidslinjeService.finnUtbetalingsTidslinjeForPeriodeId(førstePeriodeIdKjede1, utbetalingstidslinjer)
            val andreTidslinje = utbetalingsTidslinjeService.finnUtbetalingsTidslinjeForPeriodeId(førstePeriodeIdKjede2, utbetalingstidslinjer)

            // Assert
            assertThat(utbetalingstidslinjer).hasSize(2)

            assertThat(førsteTidslinje.tidslinje).isNotNull
            val perioderIFørsteTidslinje = førsteTidslinje.tidslinje.tilPerioderIkkeNull()
            assertThat(perioderIFørsteTidslinje).hasSize(2)
            assertThat(perioderIFørsteTidslinje[0].fom).isEqualTo(førstegangsBehandlingPeriode1Kjede1.vedtakdatoFom)
            assertThat(perioderIFørsteTidslinje[0].tom).isEqualTo(førstegangsBehandlingPeriode1Kjede1.vedtakdatoTom)
            assertThat(perioderIFørsteTidslinje[0].verdi.behandlingId).isEqualTo(førstegangsBehandlingPeriode1Kjede1.behandlingId)
            assertThat(perioderIFørsteTidslinje[0].verdi.periodeId).isEqualTo(førstegangsBehandlingPeriode1Kjede1.periodeId)
            assertThat(perioderIFørsteTidslinje[0].verdi.forrigePeriodeId).isEqualTo(førstegangsBehandlingPeriode1Kjede1.forrigePeriodeId)

            assertThat(perioderIFørsteTidslinje[1].fom).isEqualTo(førstegangsbehandlingPeriode2Kjede1.vedtakdatoFom)
            assertThat(perioderIFørsteTidslinje[1].tom).isEqualTo(førstegangsbehandlingPeriode2Kjede1.vedtakdatoTom)
            assertThat(perioderIFørsteTidslinje[1].verdi.behandlingId).isEqualTo(førstegangsbehandlingPeriode2Kjede1.behandlingId)
            assertThat(perioderIFørsteTidslinje[1].verdi.periodeId).isEqualTo(førstegangsbehandlingPeriode2Kjede1.periodeId)
            assertThat(perioderIFørsteTidslinje[1].verdi.forrigePeriodeId).isEqualTo(førstegangsbehandlingPeriode2Kjede1.forrigePeriodeId)

            assertThat(andreTidslinje.tidslinje).isNotNull
            val perioderIAndreTidslinje = andreTidslinje.tidslinje.tilPerioderIkkeNull()
            assertThat(perioderIAndreTidslinje).hasSize(1)
            assertThat(perioderIAndreTidslinje[0].fom).isEqualTo(førstegangsbehandlingPeriode1Kjede2.vedtakdatoFom)
            assertThat(perioderIAndreTidslinje[0].tom).isEqualTo(førstegangsbehandlingPeriode1Kjede2.vedtakdatoTom)
            assertThat(perioderIAndreTidslinje[0].verdi.behandlingId).isEqualTo(førstegangsbehandlingPeriode1Kjede2.behandlingId)
            assertThat(perioderIAndreTidslinje[0].verdi.periodeId).isEqualTo(førstegangsbehandlingPeriode1Kjede2.periodeId)
            assertThat(perioderIAndreTidslinje[0].verdi.forrigePeriodeId).isEqualTo(førstegangsbehandlingPeriode1Kjede2.forrigePeriodeId)
        }

        @Test
        fun `skal generere utbetalingstidslinjer for revurdering med endring på 1 kjede med ordinær barnetrygd`() {
            // Arrange
            val revurdering = lagBehandling(fagsak)

            val revurderingPeriode3Kjede1 =
                lagUtbetalingsperiode(
                    fom = YearMonth.of(2024, 12).førsteDagIInneværendeMåned(),
                    tom = YearMonth.of(2040, 9).sisteDagIInneværendeMåned(),
                    periodeId = 3,
                    forrigePeriodeId = 1,
                    behandlingId = revurdering.id,
                    klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                    beløp = BigDecimal.valueOf(500L),
                )
            val utbetalingsoppdragRevurdering =
                lagUtbetalingsoppdrag(
                    avstemmingTidspunkt = LocalDateTime.of(2025, 2, 1, 0, 0, 0),
                    utbetalingsperiode =
                        listOf(revurderingPeriode3Kjede1),
                )

            every { tilkjentYtelseRepository.findByFagsak(fagsak.id) } returns
                listOf(
                    lagTilkjentYtelse(behandling = førstegangsbehandling, utbetalingsoppdrag = objectMapper.writeValueAsString(lagUtbetalingsoppdragFørstegangsbehandling())),
                    lagTilkjentYtelse(behandling = revurdering, utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdragRevurdering)),
                )

            // Act
            val utbetalingstidslinjer = utbetalingsTidslinjeService.genererUtbetalingstidslinjerForFagsak(fagsakId = fagsak.id)
            val førsteTidslinje = utbetalingsTidslinjeService.finnUtbetalingsTidslinjeForPeriodeId(førstePeriodeIdKjede1, utbetalingstidslinjer)
            val andreTidslinje = utbetalingsTidslinjeService.finnUtbetalingsTidslinjeForPeriodeId(førstePeriodeIdKjede2, utbetalingstidslinjer)

            // Assert
            assertThat(utbetalingstidslinjer).hasSize(2)
            assertThat(førsteTidslinje.tidslinje).isNotNull
            val perioderIFørsteTidslinje = førsteTidslinje.tidslinje.tilPerioderIkkeNull()
            assertThat(perioderIFørsteTidslinje).hasSize(2)
            assertThat(perioderIFørsteTidslinje[0].fom).isEqualTo(førstegangsBehandlingPeriode1Kjede1.vedtakdatoFom)
            assertThat(perioderIFørsteTidslinje[0].tom).isEqualTo(revurderingPeriode3Kjede1.vedtakdatoFom.minusDays(1))
            assertThat(perioderIFørsteTidslinje[0].verdi.behandlingId).isEqualTo(førstegangsBehandlingPeriode1Kjede1.behandlingId)
            assertThat(perioderIFørsteTidslinje[0].verdi.periodeId).isEqualTo(førstegangsBehandlingPeriode1Kjede1.periodeId)
            assertThat(perioderIFørsteTidslinje[0].verdi.forrigePeriodeId).isEqualTo(førstegangsBehandlingPeriode1Kjede1.forrigePeriodeId)
            assertThat(perioderIFørsteTidslinje[1].fom).isEqualTo(revurderingPeriode3Kjede1.vedtakdatoFom)
            assertThat(perioderIFørsteTidslinje[1].tom).isEqualTo(revurderingPeriode3Kjede1.vedtakdatoTom)
            assertThat(perioderIFørsteTidslinje[1].verdi.behandlingId).isEqualTo(revurderingPeriode3Kjede1.behandlingId)
            assertThat(perioderIFørsteTidslinje[1].verdi.periodeId).isEqualTo(revurderingPeriode3Kjede1.periodeId)
            assertThat(perioderIFørsteTidslinje[1].verdi.forrigePeriodeId).isEqualTo(revurderingPeriode3Kjede1.forrigePeriodeId)

            assertThat(andreTidslinje.tidslinje).isNotNull
            val perioderIAndreTidslinje = andreTidslinje.tidslinje.tilPerioderIkkeNull()
            assertThat(perioderIAndreTidslinje).hasSize(1)
            assertThat(perioderIAndreTidslinje[0].fom).isEqualTo(førstegangsbehandlingPeriode1Kjede2.vedtakdatoFom)
            assertThat(perioderIAndreTidslinje[0].tom).isEqualTo(førstegangsbehandlingPeriode1Kjede2.vedtakdatoTom)
            assertThat(perioderIAndreTidslinje[0].verdi.behandlingId).isEqualTo(førstegangsbehandlingPeriode1Kjede2.behandlingId)
            assertThat(perioderIAndreTidslinje[0].verdi.periodeId).isEqualTo(førstegangsbehandlingPeriode1Kjede2.periodeId)
            assertThat(perioderIAndreTidslinje[0].verdi.forrigePeriodeId).isEqualTo(førstegangsbehandlingPeriode1Kjede2.forrigePeriodeId)
        }

        @Test
        fun `skal generere utbetalingstidslinjer for revurdering med opphør på 1 kjede med ordinær barnetrygd`() {
            // Arrange
            val førstegangsbehandling = lagBehandling(fagsak)
            val revurderingOpphør = lagBehandling(fagsak)

            val opphørsdato = YearMonth.of(2025, 2).førsteDagIInneværendeMåned()

            val revurderingPeriode1Kjede2 =
                lagUtbetalingsperiode(
                    fom = YearMonth.of(2023, 4).førsteDagIInneværendeMåned(),
                    tom = YearMonth.of(2040, 3).sisteDagIInneværendeMåned(),
                    periodeId = 2,
                    forrigePeriodeId = null,
                    behandlingId = revurderingOpphør.id,
                    klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                    beløp = BigDecimal.valueOf(1000L),
                    opphør = Opphør(opphørsdato),
                )
            val utbetalingsoppdragRevurderingOpphør =
                lagUtbetalingsoppdrag(
                    avstemmingTidspunkt = LocalDateTime.of(2025, 3, 1, 0, 0, 0),
                    utbetalingsperiode =
                        listOf(
                            revurderingPeriode1Kjede2,
                        ),
                )

            every { tilkjentYtelseRepository.findByFagsak(fagsak.id) } returns
                listOf(
                    lagTilkjentYtelse(behandling = førstegangsbehandling, utbetalingsoppdrag = objectMapper.writeValueAsString(lagUtbetalingsoppdragFørstegangsbehandling())),
                    lagTilkjentYtelse(behandling = revurderingOpphør, utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdragRevurderingOpphør)),
                )

            // Act
            val utbetalingstidslinjer = utbetalingsTidslinjeService.genererUtbetalingstidslinjerForFagsak(fagsakId = fagsak.id)
            val førsteTidslinje = utbetalingsTidslinjeService.finnUtbetalingsTidslinjeForPeriodeId(førstePeriodeIdKjede1, utbetalingstidslinjer)
            val andreTidslinje = utbetalingsTidslinjeService.finnUtbetalingsTidslinjeForPeriodeId(førstePeriodeIdKjede2, utbetalingstidslinjer)

            // Assert
            assertThat(utbetalingstidslinjer).hasSize(2)

            assertThat(førsteTidslinje.tidslinje).isNotNull
            val perioderIFørsteTidslinje = førsteTidslinje.tidslinje.tilPerioderIkkeNull()
            assertThat(perioderIFørsteTidslinje).hasSize(2)
            assertThat(perioderIFørsteTidslinje[0].fom).isEqualTo(førstegangsBehandlingPeriode1Kjede1.vedtakdatoFom)
            assertThat(perioderIFørsteTidslinje[0].tom).isEqualTo(førstegangsBehandlingPeriode1Kjede1.vedtakdatoTom)
            assertThat(perioderIFørsteTidslinje[0].verdi.behandlingId).isEqualTo(førstegangsBehandlingPeriode1Kjede1.behandlingId)
            assertThat(perioderIFørsteTidslinje[0].verdi.periodeId).isEqualTo(førstegangsBehandlingPeriode1Kjede1.periodeId)
            assertThat(perioderIFørsteTidslinje[0].verdi.forrigePeriodeId).isEqualTo(førstegangsBehandlingPeriode1Kjede1.forrigePeriodeId)

            assertThat(perioderIFørsteTidslinje[1].fom).isEqualTo(førstegangsbehandlingPeriode2Kjede1.vedtakdatoFom)
            assertThat(perioderIFørsteTidslinje[1].tom).isEqualTo(førstegangsbehandlingPeriode2Kjede1.vedtakdatoTom)
            assertThat(perioderIFørsteTidslinje[1].verdi.behandlingId).isEqualTo(førstegangsbehandlingPeriode2Kjede1.behandlingId)
            assertThat(perioderIFørsteTidslinje[1].verdi.periodeId).isEqualTo(førstegangsbehandlingPeriode2Kjede1.periodeId)
            assertThat(perioderIFørsteTidslinje[1].verdi.forrigePeriodeId).isEqualTo(førstegangsbehandlingPeriode2Kjede1.forrigePeriodeId)

            assertThat(andreTidslinje.tidslinje).isNotNull
            val perioderIAndreTidslinje = andreTidslinje.tidslinje.tilPerioderIkkeNull()
            assertThat(perioderIAndreTidslinje).hasSize(1)
            assertThat(perioderIAndreTidslinje[0].fom).isEqualTo(revurderingPeriode1Kjede2.vedtakdatoFom)
            assertThat(perioderIAndreTidslinje[0].tom).isEqualTo(opphørsdato.minusDays(1))
            assertThat(perioderIAndreTidslinje[0].verdi.behandlingId).isEqualTo(revurderingPeriode1Kjede2.behandlingId)
            assertThat(perioderIAndreTidslinje[0].verdi.periodeId).isEqualTo(revurderingPeriode1Kjede2.periodeId)
            assertThat(perioderIAndreTidslinje[0].verdi.forrigePeriodeId).isEqualTo(revurderingPeriode1Kjede2.forrigePeriodeId)
        }

        @Test
        fun `skal generere utbetalingstidslinjer for revurdering med opphør på 1 kjede med ordinær barnetrygd hvor opphørsdato er før periodens fom`() {
            // Arrange
            val revurderingOpphør = lagBehandling(fagsak)

            val revurderingPeriode2Kjede1 =
                lagUtbetalingsperiode(
                    fom = YearMonth.of(2025, 2).førsteDagIInneværendeMåned(),
                    tom = YearMonth.of(2040, 9).sisteDagIInneværendeMåned(),
                    periodeId = 1,
                    forrigePeriodeId = 0,
                    behandlingId = revurderingOpphør.id,
                    klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                    beløp = BigDecimal.valueOf(500L),
                    opphør = Opphør(YearMonth.of(2024, 11).førsteDagIInneværendeMåned()),
                )

            val utbetalingsoppdragRevurderingOpphør =
                lagUtbetalingsoppdrag(
                    avstemmingTidspunkt = LocalDateTime.of(2025, 3, 1, 0, 0, 0),
                    utbetalingsperiode =
                        listOf(revurderingPeriode2Kjede1),
                )

            every { tilkjentYtelseRepository.findByFagsak(fagsak.id) } returns
                listOf(
                    lagTilkjentYtelse(behandling = førstegangsbehandling, utbetalingsoppdrag = objectMapper.writeValueAsString(lagUtbetalingsoppdragFørstegangsbehandling())),
                    lagTilkjentYtelse(behandling = revurderingOpphør, utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdragRevurderingOpphør)),
                )

            // Act
            val utbetalingstidslinjer = utbetalingsTidslinjeService.genererUtbetalingstidslinjerForFagsak(fagsakId = fagsak.id)
            val førsteTidslinje = utbetalingsTidslinjeService.finnUtbetalingsTidslinjeForPeriodeId(førstePeriodeIdKjede1, utbetalingstidslinjer)
            val andreTidslinje = utbetalingsTidslinjeService.finnUtbetalingsTidslinjeForPeriodeId(førstePeriodeIdKjede2, utbetalingstidslinjer)

            // Assert
            assertThat(utbetalingstidslinjer).hasSize(2)
            assertThat(førsteTidslinje.tidslinje).isNotNull
            val perioderIFørsteTidslinje = førsteTidslinje.tidslinje.tilPerioderIkkeNull()
            assertThat(perioderIFørsteTidslinje).hasSize(1)
            assertThat(perioderIFørsteTidslinje[0].fom).isEqualTo(førstegangsBehandlingPeriode1Kjede1.vedtakdatoFom)
            assertThat(perioderIFørsteTidslinje[0].tom).isEqualTo(revurderingPeriode2Kjede1.opphør!!.opphørDatoFom.minusDays(1))
            assertThat(perioderIFørsteTidslinje[0].verdi.behandlingId).isEqualTo(førstegangsBehandlingPeriode1Kjede1.behandlingId)
            assertThat(perioderIFørsteTidslinje[0].verdi.periodeId).isEqualTo(førstegangsBehandlingPeriode1Kjede1.periodeId)
            assertThat(perioderIFørsteTidslinje[0].verdi.forrigePeriodeId).isEqualTo(førstegangsBehandlingPeriode1Kjede1.forrigePeriodeId)

            assertThat(andreTidslinje.tidslinje).isNotNull
            val perioderIAndreTidslinje = andreTidslinje.tidslinje.tilPerioderIkkeNull()
            assertThat(perioderIAndreTidslinje).hasSize(1)
            assertThat(perioderIAndreTidslinje[0].fom).isEqualTo(førstegangsbehandlingPeriode1Kjede2.vedtakdatoFom)
            assertThat(perioderIAndreTidslinje[0].tom).isEqualTo(førstegangsbehandlingPeriode1Kjede2.vedtakdatoTom)
            assertThat(perioderIAndreTidslinje[0].verdi.behandlingId).isEqualTo(førstegangsbehandlingPeriode1Kjede2.behandlingId)
            assertThat(perioderIAndreTidslinje[0].verdi.periodeId).isEqualTo(førstegangsbehandlingPeriode1Kjede2.periodeId)
            assertThat(perioderIAndreTidslinje[0].verdi.forrigePeriodeId).isEqualTo(førstegangsbehandlingPeriode1Kjede2.forrigePeriodeId)
        }

        @Test
        fun `skal generere utbetalingstidslinjer for revurdering med opphør og hull før nye andeler`() {
            // Arrange
            val fagsak = lagFagsak()
            val førstegangsbehandling = lagBehandling(fagsak)
            val revurderingOpphør = lagBehandling(fagsak)

            val revurderingPeriode2Kjede1 =
                lagUtbetalingsperiode(
                    fom = YearMonth.of(2025, 2).førsteDagIInneværendeMåned(),
                    tom = YearMonth.of(2040, 9).sisteDagIInneværendeMåned(),
                    periodeId = 1,
                    forrigePeriodeId = 0,
                    behandlingId = revurderingOpphør.id,
                    klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                    beløp = BigDecimal.valueOf(500L),
                    opphør = Opphør(YearMonth.of(2024, 11).førsteDagIInneværendeMåned()),
                )

            val revurderingPeriode3Kjede1 =
                lagUtbetalingsperiode(
                    fom = YearMonth.of(2025, 2).førsteDagIInneværendeMåned(),
                    tom = YearMonth.of(2040, 9).sisteDagIInneværendeMåned(),
                    periodeId = 3,
                    forrigePeriodeId = 1,
                    behandlingId = revurderingOpphør.id,
                    klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
                    beløp = BigDecimal.valueOf(1000L),
                )

            val utbetalingsoppdragRevurderingOpphør =
                lagUtbetalingsoppdrag(
                    avstemmingTidspunkt = LocalDateTime.of(2025, 3, 1, 0, 0, 0),
                    utbetalingsperiode =
                        listOf(
                            revurderingPeriode2Kjede1,
                            revurderingPeriode3Kjede1,
                        ),
                )

            every { tilkjentYtelseRepository.findByFagsak(fagsak.id) } returns
                listOf(
                    lagTilkjentYtelse(behandling = førstegangsbehandling, utbetalingsoppdrag = objectMapper.writeValueAsString(lagUtbetalingsoppdragFørstegangsbehandling())),
                    lagTilkjentYtelse(behandling = revurderingOpphør, utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdragRevurderingOpphør)),
                )

            // Act
            val utbetalingstidslinjer = utbetalingsTidslinjeService.genererUtbetalingstidslinjerForFagsak(fagsakId = fagsak.id)
            val førsteTidslinje = utbetalingsTidslinjeService.finnUtbetalingsTidslinjeForPeriodeId(førstePeriodeIdKjede1, utbetalingstidslinjer)
            val andreTidslinje = utbetalingsTidslinjeService.finnUtbetalingsTidslinjeForPeriodeId(førstePeriodeIdKjede2, utbetalingstidslinjer)

            // Assert
            assertThat(utbetalingstidslinjer).hasSize(2)
            assertThat(førsteTidslinje.tidslinje).isNotNull
            val perioderIFørsteTidslinje = førsteTidslinje.tidslinje.tilPerioderIkkeNull()
            assertThat(perioderIFørsteTidslinje).hasSize(2)
            assertThat(perioderIFørsteTidslinje[0].fom).isEqualTo(førstegangsBehandlingPeriode1Kjede1.vedtakdatoFom)
            assertThat(perioderIFørsteTidslinje[0].tom).isEqualTo(revurderingPeriode2Kjede1.opphør!!.opphørDatoFom.minusDays(1))
            assertThat(perioderIFørsteTidslinje[0].verdi.behandlingId).isEqualTo(førstegangsBehandlingPeriode1Kjede1.behandlingId)
            assertThat(perioderIFørsteTidslinje[0].verdi.periodeId).isEqualTo(førstegangsBehandlingPeriode1Kjede1.periodeId)
            assertThat(perioderIFørsteTidslinje[0].verdi.forrigePeriodeId).isEqualTo(førstegangsBehandlingPeriode1Kjede1.forrigePeriodeId)
            assertThat(perioderIFørsteTidslinje[1].fom).isEqualTo(revurderingPeriode3Kjede1.vedtakdatoFom)
            assertThat(perioderIFørsteTidslinje[1].tom).isEqualTo(revurderingPeriode3Kjede1.vedtakdatoTom)
            assertThat(perioderIFørsteTidslinje[1].verdi.behandlingId).isEqualTo(revurderingPeriode3Kjede1.behandlingId)
            assertThat(perioderIFørsteTidslinje[1].verdi.periodeId).isEqualTo(revurderingPeriode3Kjede1.periodeId)
            assertThat(perioderIFørsteTidslinje[1].verdi.forrigePeriodeId).isEqualTo(revurderingPeriode3Kjede1.forrigePeriodeId)

            assertThat(andreTidslinje.tidslinje).isNotNull
            val perioderIAndreTidslinje = andreTidslinje.tidslinje.tilPerioderIkkeNull()
            assertThat(perioderIAndreTidslinje).hasSize(1)
            assertThat(perioderIAndreTidslinje[0].fom).isEqualTo(førstegangsbehandlingPeriode1Kjede2.vedtakdatoFom)
            assertThat(perioderIAndreTidslinje[0].tom).isEqualTo(førstegangsbehandlingPeriode1Kjede2.vedtakdatoTom)
            assertThat(perioderIAndreTidslinje[0].verdi.behandlingId).isEqualTo(førstegangsbehandlingPeriode1Kjede2.behandlingId)
            assertThat(perioderIAndreTidslinje[0].verdi.periodeId).isEqualTo(førstegangsbehandlingPeriode1Kjede2.periodeId)
            assertThat(perioderIAndreTidslinje[0].verdi.forrigePeriodeId).isEqualTo(førstegangsbehandlingPeriode1Kjede2.forrigePeriodeId)
        }
    }

    private val førstegangsBehandlingPeriode1Kjede1 =
        lagUtbetalingsperiode(
            fom = YearMonth.of(2024, 10).førsteDagIInneværendeMåned(),
            tom = YearMonth.of(2025, 1).sisteDagIInneværendeMåned(),
            periodeId = førstePeriodeIdKjede1,
            forrigePeriodeId = null,
            behandlingId = førstegangsbehandling.id,
            klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
            beløp = BigDecimal.valueOf(1000L),
        )

    private val førstegangsbehandlingPeriode2Kjede1 =
        lagUtbetalingsperiode(
            fom = YearMonth.of(2025, 2).førsteDagIInneværendeMåned(),
            tom = YearMonth.of(2040, 9).sisteDagIInneværendeMåned(),
            periodeId = 1,
            forrigePeriodeId = 0,
            behandlingId = førstegangsbehandling.id,
            klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
            beløp = BigDecimal.valueOf(500L),
        )

    private val førstegangsbehandlingPeriode1Kjede2 =
        lagUtbetalingsperiode(
            fom = YearMonth.of(2023, 4).førsteDagIInneværendeMåned(),
            tom = YearMonth.of(2040, 3).sisteDagIInneværendeMåned(),
            periodeId = førstePeriodeIdKjede2,
            forrigePeriodeId = null,
            behandlingId = førstegangsbehandling.id,
            klassifisering = YtelseType.ORDINÆR_BARNETRYGD.klassifisering,
            beløp = BigDecimal.valueOf(1000L),
        )

    private fun lagUtbetalingsoppdragFørstegangsbehandling(): Utbetalingsoppdrag =
        lagUtbetalingsoppdrag(
            avstemmingTidspunkt = LocalDateTime.of(2025, 1, 1, 0, 0, 0),
            utbetalingsperiode =
                listOf(
                    førstegangsBehandlingPeriode1Kjede1,
                    førstegangsbehandlingPeriode2Kjede1,
                    førstegangsbehandlingPeriode1Kjede2,
                ),
        )
}

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
