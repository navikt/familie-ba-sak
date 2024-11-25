package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.YearMonth

class StarttidspunktForUtbetalingstabellServiceTest {
    private val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val endretUtbetalingAndelRepository = mockk<EndretUtbetalingAndelRepository>()
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()

    private val starttidspunktForUtbetalingstabellService =
        StarttidspunktForUtbetalingstabellService(
            vedtaksperiodeService = vedtaksperiodeService,
            endretUtbetalingAndelRepository = endretUtbetalingAndelRepository,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
        )

    @ParameterizedTest
    @EnumSource(BehandlingÅrsak::class, mode = EnumSource.Mode.EXCLUDE, names = ["ÅRLIG_KONTROLL"])
    fun `finnStarttidspunktForUtbetalingstabell returnerer endringstidspunkt for alle behandlingsårsaker utenom ÅRLIG_KONTROLL`(
        behandlingÅrsak: BehandlingÅrsak,
    ) {
        every { vedtaksperiodeService.finnEndringstidspunktForBehandling(any()) } returns LocalDate.of(2020, 1, 1)

        val behandling = lagBehandling(årsak = behandlingÅrsak)

        val starttidspunkt = starttidspunktForUtbetalingstabellService.finnStarttidspunktForUtbetalingstabell(behandling)

        assertThat(starttidspunkt).isEqualTo(LocalDate.of(2020, 1, 1))
    }

    @Test
    fun `finnStarttidspunktForUtbetalingstabell returnerer endringstidspunkt for behandlingsårsak ÅRLIG_KONTROLL, dersom endringstidspunkt er tidligere enn 1 januar i fjor`() {
        every { vedtaksperiodeService.finnEndringstidspunktForBehandling(any()) } returns LocalDate.of(2020, 1, 1)

        val behandling = lagBehandling(årsak = BehandlingÅrsak.ÅRLIG_KONTROLL)

        val starttidspunkt = starttidspunktForUtbetalingstabellService.finnStarttidspunktForUtbetalingstabell(behandling)

        assertThat(starttidspunkt).isEqualTo(LocalDate.of(2020, 1, 1))
    }

    @Test
    fun `finnStarttidspunktForUtbetalingstabell returnerer tidligst 1 januar i fjor for behandlingsårsak ÅRLIG_KONTROLL, dersom endringstidspunkt er TIDENES_ENDE`() {
        every { vedtaksperiodeService.finnEndringstidspunktForBehandling(any()) } returns TIDENES_ENDE
        every { endretUtbetalingAndelRepository.findByBehandlingId(any()) } returns emptyList()
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 1),
                    tom = YearMonth.now().plusYears(1),
                ),
            )

        val behandling = lagBehandling(årsak = BehandlingÅrsak.ÅRLIG_KONTROLL)

        val starttidspunkt = starttidspunktForUtbetalingstabellService.finnStarttidspunktForUtbetalingstabell(behandling)

        assertThat(starttidspunkt).isEqualTo(LocalDate.now().minusYears(1).withDayOfYear(1))
    }

    @Test
    fun `finnStarttidspunktForUtbetalingstabell returnerer 1 januar i fjor for behandlingsårsak ÅRLIG_KONTROLL, selv om endringstidspunkt er senere`() {
        every { vedtaksperiodeService.finnEndringstidspunktForBehandling(any()) } returns LocalDate.of(2024, 1, 1)
        every { endretUtbetalingAndelRepository.findByBehandlingId(any()) } returns emptyList()
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 1),
                    tom = YearMonth.now().plusYears(1),
                ),
            )

        val behandling = lagBehandling(årsak = BehandlingÅrsak.ÅRLIG_KONTROLL)

        val starttidspunkt = starttidspunktForUtbetalingstabellService.finnStarttidspunktForUtbetalingstabell(behandling)

        assertThat(starttidspunkt).isEqualTo(LocalDate.now().minusYears(1).withDayOfYear(1))
    }

    @Test
    fun `finnStarttidspunktForUtbetalingstabell returnerer første utbetalingstidspunkt ved ÅRLIG_KONTROLL dersom endringstidspunkt er TIDENES_ENDE og første utbetaling er etter 1 januar i fjor`() {
        every { vedtaksperiodeService.finnEndringstidspunktForBehandling(any()) } returns TIDENES_ENDE
        every { endretUtbetalingAndelRepository.findByBehandlingId(any()) } returns emptyList()
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(any()) } returns
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2024, 5),
                    tom = YearMonth.now().plusYears(1),
                ),
            )

        val behandling = lagBehandling(årsak = BehandlingÅrsak.ÅRLIG_KONTROLL)

        val starttidspunkt = starttidspunktForUtbetalingstabellService.finnStarttidspunktForUtbetalingstabell(behandling)

        assertThat(starttidspunkt).isEqualTo(LocalDate.of(2024, 5, 1))
    }
}
