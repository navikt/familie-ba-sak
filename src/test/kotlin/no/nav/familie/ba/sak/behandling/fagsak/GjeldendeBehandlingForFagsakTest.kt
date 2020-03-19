package no.nav.familie.ba.sak.behandling.fagsak

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.BeregningResultat
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagRevurdering
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate


class GjeldendeBehandlingForFagsakTest {

    private val UTBETALINGSMÅNED = LocalDate.now()
    private val FAGSAK_ID = defaultFagsak.id
    private val beregningService = mockk<BeregningService>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private lateinit var behandlingService: BehandlingService

    @BeforeEach
    fun setUp() {
        behandlingService = BehandlingService(
                behandlingRepository,
                mockk(),
                beregningService,
                mockk()
        )
    }

    @Test
    fun `Skal oppdatere gjeldende behandling FGB`() {
        val behandling = lagBehandling()
        every { behandlingRepository.findByFagsakAndFerdigstiltOrIverksatt(any()) } returns listOf(behandling)
        every { behandlingRepository.save(any<Behandling>()) } returns behandling
        every { beregningService.hentBeregningsresultatForBehandling(any()) } returns BeregningResultat(
                behandling = behandling,
                stønadFom = UTBETALINGSMÅNED.minusMonths(1),
                stønadTom = UTBETALINGSMÅNED.plusMonths(3),
                utbetalingsoppdrag = "",
                erOpphør = false,
                opprettetDato = behandling.opprettetTidspunkt.toLocalDate()
        )

        val gjeldendeBehandling = behandlingService.oppdaterGjeldendeBehandlingForNesteUtbetaling(FAGSAK_ID, UTBETALINGSMÅNED)

        Assertions.assertNotNull(gjeldendeBehandling)
        Assertions.assertEquals(behandling.id, gjeldendeBehandling!!.id)
        Assertions.assertEquals(behandling.type, gjeldendeBehandling!!.type)
    }

    @Test
    fun `Skal oppdatere gjeldende behandling for FGB med opphør frem i tid`() {
        val behandling = lagBehandling()
        val opphør = lagRevurdering()
        every { behandlingRepository.findByFagsakAndFerdigstiltOrIverksatt(any()) } returns listOf(behandling, opphør)
        every { behandlingRepository.save(any<Behandling>()) } returns behandling
        every { beregningService.hentBeregningsresultatForBehandling(behandling.id) } returns BeregningResultat(
                behandling = behandling,
                stønadFom = UTBETALINGSMÅNED.minusMonths(1),
                stønadTom = UTBETALINGSMÅNED.plusMonths(3),
                utbetalingsoppdrag = "",
                erOpphør = false,
                opprettetDato = behandling.opprettetTidspunkt.toLocalDate()
        )
        every { beregningService.hentBeregningsresultatForBehandling(opphør.id) } returns BeregningResultat(
                behandling = opphør,
                stønadFom = UTBETALINGSMÅNED.plusMonths(2),
                stønadTom = UTBETALINGSMÅNED.plusMonths(3),
                utbetalingsoppdrag = "",
                erOpphør = true,
                opprettetDato = opphør.opprettetTidspunkt.toLocalDate()
        )

        val gjeldendeBehandling = behandlingService.oppdaterGjeldendeBehandlingForNesteUtbetaling(FAGSAK_ID, UTBETALINGSMÅNED)

        Assertions.assertNotNull(gjeldendeBehandling)
        Assertions.assertEquals(behandling.id, gjeldendeBehandling!!.id)
        Assertions.assertEquals(behandling.type, gjeldendeBehandling!!.type)
    }

    @Test
    fun `Skal oppdatere gjeldende behandling for FGB med opphør denne måneden`() {
        val behandling = lagBehandling()
        val opphør = lagRevurdering()
        every { behandlingRepository.findByFagsakAndFerdigstiltOrIverksatt(any()) } returns listOf(behandling, opphør)
        every { behandlingRepository.save(any<Behandling>()) } returns behandling
        every { beregningService.hentBeregningsresultatForBehandling(behandling.id) } returns BeregningResultat(
                behandling = behandling,
                stønadFom = UTBETALINGSMÅNED.minusMonths(1),
                stønadTom = UTBETALINGSMÅNED.plusMonths(3),
                utbetalingsoppdrag = "",
                erOpphør = false,
                opprettetDato = behandling.opprettetTidspunkt.toLocalDate()
        )
        every { beregningService.hentBeregningsresultatForBehandling(opphør.id) } returns BeregningResultat(
                behandling = opphør,
                stønadFom = UTBETALINGSMÅNED.withDayOfMonth(1),
                stønadTom = UTBETALINGSMÅNED.plusMonths(3),
                utbetalingsoppdrag = "",
                erOpphør = true,
                opprettetDato = opphør.opprettetTidspunkt.toLocalDate()
        )

        val gjeldendeBehandling = behandlingService.oppdaterGjeldendeBehandlingForNesteUtbetaling(FAGSAK_ID, UTBETALINGSMÅNED)

        Assertions.assertNull(gjeldendeBehandling)
    }

    @Test
    fun `Skal oppdatere gjeldende behandling for revurdering frem i tid`() {
        val behandling = lagBehandling()
        val revurdering = lagRevurdering()
        every { behandlingRepository.findByFagsakAndFerdigstiltOrIverksatt(any()) } returns listOf(behandling, revurdering)
        every { behandlingRepository.save(any<Behandling>()) } returns behandling
        every { beregningService.hentBeregningsresultatForBehandling(behandling.id) } returns BeregningResultat(
                behandling = behandling,
                stønadFom = UTBETALINGSMÅNED.minusMonths(1),
                stønadTom = UTBETALINGSMÅNED.plusMonths(3),
                utbetalingsoppdrag = "",
                erOpphør = false,
                opprettetDato = behandling.opprettetTidspunkt.toLocalDate()
        )
        every { beregningService.hentBeregningsresultatForBehandling(revurdering.id) } returns BeregningResultat(
                behandling = revurdering,
                stønadFom = UTBETALINGSMÅNED.plusMonths(2),
                stønadTom = UTBETALINGSMÅNED.plusMonths(3),
                utbetalingsoppdrag = "",
                erOpphør = false,
                opprettetDato = revurdering.opprettetTidspunkt.toLocalDate()
        )

        val gjeldendeBehandling = behandlingService.oppdaterGjeldendeBehandlingForNesteUtbetaling(FAGSAK_ID, UTBETALINGSMÅNED)

        Assertions.assertNotNull(gjeldendeBehandling)
        Assertions.assertEquals(behandling.id, gjeldendeBehandling!!.id)
        Assertions.assertEquals(behandling.type, gjeldendeBehandling.type)
    }

    @Test
    fun `Skal oppdatere gjeldende behandling for revurdering denne måneden`() {
        val behandling = lagBehandling()
        val revurdering = lagRevurdering()
        every { behandlingRepository.findByFagsakAndFerdigstiltOrIverksatt(any()) } returns listOf(behandling, revurdering)
        every { behandlingRepository.save(any<Behandling>()) } returns behandling
        every { beregningService.hentBeregningsresultatForBehandling(behandling.id) } returns BeregningResultat(
                behandling = behandling,
                stønadFom = UTBETALINGSMÅNED.minusMonths(1),
                stønadTom = UTBETALINGSMÅNED.plusMonths(3),
                utbetalingsoppdrag = "",
                erOpphør = false,
                opprettetDato = behandling.opprettetTidspunkt.toLocalDate()
        )
        every { beregningService.hentBeregningsresultatForBehandling(revurdering.id) } returns BeregningResultat(
                behandling = revurdering,
                stønadFom = UTBETALINGSMÅNED.withDayOfMonth(1),
                stønadTom = UTBETALINGSMÅNED.plusMonths(3),
                utbetalingsoppdrag = "",
                erOpphør = false,
                opprettetDato = revurdering.opprettetTidspunkt.toLocalDate()
        )

        val gjeldendeBehandling = behandlingService.oppdaterGjeldendeBehandlingForNesteUtbetaling(FAGSAK_ID, UTBETALINGSMÅNED)

        Assertions.assertNotNull(gjeldendeBehandling)
        Assertions.assertEquals(revurdering.id, gjeldendeBehandling!!.id)
        Assertions.assertEquals(revurdering.type, gjeldendeBehandling.type)
    }

    @Test
    fun `Skal oppdatere gjeldende behandling for avsluttet behandling`() {
        val behandling = lagBehandling()
        every { behandlingRepository.findByFagsakAndFerdigstiltOrIverksatt(any()) } returns listOf(behandling)
        every { behandlingRepository.save(any<Behandling>()) } returns behandling
        every { beregningService.hentBeregningsresultatForBehandling(any()) } returns BeregningResultat(
                behandling = behandling,
                stønadFom = UTBETALINGSMÅNED.minusMonths(4),
                stønadTom = UTBETALINGSMÅNED.minusMonths(1),
                utbetalingsoppdrag = "",
                erOpphør = false,
                opprettetDato = behandling.opprettetTidspunkt.toLocalDate()
        )

        val gjeldendeBehandling = behandlingService.oppdaterGjeldendeBehandlingForNesteUtbetaling(FAGSAK_ID, UTBETALINGSMÅNED)

        Assertions.assertNull(gjeldendeBehandling)
    }
}