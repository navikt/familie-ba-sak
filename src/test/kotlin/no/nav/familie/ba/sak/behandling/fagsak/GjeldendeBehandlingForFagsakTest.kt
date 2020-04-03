package no.nav.familie.ba.sak.behandling.fagsak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.beregning.lagTestUtbetalingsoppdragForFGB
import no.nav.familie.ba.sak.beregning.lagTestUtbetalingsoppdragForOpphør
import no.nav.familie.ba.sak.beregning.lagTestUtbetalingsoppdragForRevurdering
import no.nav.familie.ba.sak.common.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate


@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres")
@Tag("integration")
class GjeldendeBehandlingForFagsakTest {

    private val UTBETALINGSMÅNED = LocalDate.now()

    @Autowired
    private lateinit var beregningService: BeregningService

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Test
    fun `Skal oppdatere gjeldende behandling FGB`() {
        val morId = randomFnr()
        val vedtakDato = LocalDate.now()
        val stønadFom = UTBETALINGSMÅNED.minusMonths(1)
        val stønadTom = UTBETALINGSMÅNED.plusMonths(3)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(morId)
        val behandling = lagFerdigstiltFGB(fagsak, morId, vedtakDato, stønadFom, stønadTom)

        val gjeldendeBehandlinger = behandlingService.oppdaterGjeldendeBehandlingForFremtidigUtbetaling(fagsak.id, vedtakDato)

        Assertions.assertEquals(1, gjeldendeBehandlinger.size)
        Assertions.assertEquals(behandling.id, gjeldendeBehandlinger[0].id)
        Assertions.assertEquals(behandling.type, gjeldendeBehandlinger[0].type)
    }

    @Test
    fun `Skal oppdatere gjeldende behandling for FGB med opphør frem i tid`() {
        val morId = randomFnr()
        val vedtakDato = LocalDate.now()
        val opphørFom = UTBETALINGSMÅNED.plusMonths(2)
        val stønadFom = UTBETALINGSMÅNED.minusMonths(1)
        val stønadTom = UTBETALINGSMÅNED.plusMonths(3)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(morId)
        val behandling = lagFerdigstiltFGB(fagsak, morId, vedtakDato, stønadFom, stønadTom)

        val opphør = behandlingService.opprettBehandling(nyRevurdering(morId))
        opprettTilkjentYtelseForBehandling(opphør)
        val utbetalingsoppdragOpphør = lagTestUtbetalingsoppdragForOpphør(
                morId,
                fagsak.id.toString(),
                behandling.id,
                vedtakDato,
                stønadFom,
                stønadTom,
                opphørFom
        )
        beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(opphør, utbetalingsoppdragOpphør)
        behandlingService.oppdaterStatusPåBehandling(opphør.id, BehandlingStatus.IVERKSATT)

        val gjeldendeBehandlinger = behandlingService.oppdaterGjeldendeBehandlingForFremtidigUtbetaling(fagsak.id, vedtakDato)

        Assertions.assertEquals(1, gjeldendeBehandlinger.size)
        Assertions.assertEquals(behandling.id, gjeldendeBehandlinger[0].id)
        Assertions.assertEquals(behandling.type, gjeldendeBehandlinger[0].type)
    }

    @Test
    fun `Skal oppdatere gjeldende behandling for FGB med opphør denne måneden`() {
        val morId = randomFnr()
        val vedtakDato = LocalDate.now()
        val opphørFom = UTBETALINGSMÅNED.withDayOfMonth(1)
        val stønadFom = UTBETALINGSMÅNED.minusMonths(1)
        val stønadTom = UTBETALINGSMÅNED.plusMonths(3)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(morId)
        val behandling = lagFerdigstiltFGB(fagsak, morId, vedtakDato, stønadFom, stønadTom)

        val opphør = behandlingService.opprettBehandling(nyRevurdering(morId))
        opprettTilkjentYtelseForBehandling(opphør)
        val utbetalingsoppdragOpphør = lagTestUtbetalingsoppdragForOpphør(
                morId,
                fagsak.id.toString(),
                behandling.id,
                vedtakDato,
                stønadFom,
                stønadTom,
                opphørFom
        )
        beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(opphør, utbetalingsoppdragOpphør)
        behandlingService.oppdaterStatusPåBehandling(opphør.id, BehandlingStatus.IVERKSATT)

        val gjeldendeBehandlinger = behandlingService.oppdaterGjeldendeBehandlingForFremtidigUtbetaling(fagsak.id, vedtakDato)

        Assertions.assertTrue(gjeldendeBehandlinger.isEmpty())
    }

    @Test
    fun `Sett riktig gjeldende behandling for revurdering frem i tid`() {
        val morId = randomFnr()
        val vedtakDato = LocalDate.now()
        val opphørFom = UTBETALINGSMÅNED.plusMonths(2)
        val stønadFom = UTBETALINGSMÅNED.minusMonths(1)
        val revurderingFom = UTBETALINGSMÅNED.plusMonths(2)
        val stønadTom = UTBETALINGSMÅNED.plusMonths(3)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(morId)
        val behandling = lagFerdigstiltFGB(fagsak, morId, vedtakDato, stønadFom, stønadTom)

        val revurdering = behandlingService.opprettBehandling(nyRevurdering(morId))
        opprettTilkjentYtelseForBehandling(revurdering)
        val utbetalingsoppdragRevurdering = lagTestUtbetalingsoppdragForRevurdering(
                morId,
                fagsak.id.toString(),
                revurdering.id,
                behandling.id,
                vedtakDato,
                opphørFom,
                stønadTom,
                revurderingFom
        )
        beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(revurdering, utbetalingsoppdragRevurdering)
        behandlingService.oppdaterStatusPåBehandling(revurdering.id, BehandlingStatus.IVERKSATT)

        val gjeldendeBehandlinger = behandlingService.oppdaterGjeldendeBehandlingForFremtidigUtbetaling(fagsak.id, vedtakDato)

        Assertions.assertEquals(2, gjeldendeBehandlinger.size)
    }

    @Test
    fun `Sett riktig gjeldende behandling ved revurdering denne måneden`() {
        val morId = randomFnr()
        val vedtakDato = LocalDate.now()
        val opphørFom = UTBETALINGSMÅNED.withDayOfMonth(1)
        val stønadFom = UTBETALINGSMÅNED.minusMonths(1)
        val revurderingFom = UTBETALINGSMÅNED.plusMonths(2)
        val stønadTom = UTBETALINGSMÅNED.plusMonths(3)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(morId)
        val behandling = lagFerdigstiltFGB(fagsak, morId, vedtakDato, stønadFom, stønadTom)


        val revurdering = behandlingService.opprettBehandling(nyRevurdering(morId))
        opprettTilkjentYtelseForBehandling(revurdering)
        val utbetalingsoppdragRevurdering = lagTestUtbetalingsoppdragForRevurdering(
                morId,
                fagsak.id.toString(),
                revurdering.id,
                behandling.id,
                stønadFom,
                opphørFom,
                stønadTom,
                revurderingFom
        )
        beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(revurdering, utbetalingsoppdragRevurdering)
        behandlingService.oppdaterStatusPåBehandling(revurdering.id, BehandlingStatus.IVERKSATT)

        val gjeldendeBehandlinger = behandlingService.oppdaterGjeldendeBehandlingForFremtidigUtbetaling(fagsak.id, vedtakDato)

        Assertions.assertEquals(1, gjeldendeBehandlinger.size)
        Assertions.assertEquals(revurdering.id, gjeldendeBehandlinger[0].id)
        Assertions.assertEquals(revurdering.type, gjeldendeBehandlinger[0].type)
    }

    @Test
    fun `Skal oppdatere gjeldende behandling for avsluttet behandling`() {
        val morId = randomFnr()
        val vedtakDato = LocalDate.now()
        val stønadFom = UTBETALINGSMÅNED.minusMonths(4)
        val stønadTom = UTBETALINGSMÅNED.minusMonths(1)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(morId)
        lagFerdigstiltFGB(fagsak, morId, vedtakDato, stønadFom, stønadTom)

        val gjeldendeBehandlinger = behandlingService.oppdaterGjeldendeBehandlingForFremtidigUtbetaling(fagsak.id, vedtakDato)

        Assertions.assertTrue(gjeldendeBehandlinger.isEmpty())
    }

    private fun lagFerdigstiltFGB(fagsak: Fagsak, personIdent: String, vedtakDato: LocalDate, stønadFom: LocalDate, stønadTom: LocalDate): Behandling {
        val behandling = behandlingService.opprettBehandling(nyOrdinærBehandling(personIdent))
        opprettTilkjentYtelseForBehandling(behandling)
        val utbetalingsoppdrag = lagTestUtbetalingsoppdragForFGB(
                personIdent,
                fagsak.id.toString(),
                behandling.id,
                vedtakDato,
                stønadFom,
                stønadTom
        )
        beregningService.oppdaterTilkjentYtelseMedUtbetalingsoppdrag(behandling, utbetalingsoppdrag)
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.FERDIGSTILT)
        return behandling
    }

    private fun opprettTilkjentYtelseForBehandling(behandling: Behandling) {
        tilkjentYtelseRepository.save(lagInitiellTilkjentYtelse(behandling))
    }
}